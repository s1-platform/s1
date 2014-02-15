package org.s1.cluster.node;

import org.s1.cluster.datasource.DistributedDataSource;
import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.BindException;
import java.util.List;
import java.util.Map;


/**
 * Cluster Node
 * Log levels:
 * <pre>ERROR - error on DataSource
 * WARN - error initializing DistributedDataSource
 * INFO - start, stop, shutdownOnError
 * </pre>
 */
public class ClusterNode {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterNode.class);

    private static String nodeId;
    private static volatile String status = "stopped";
    private static volatile boolean initialized = false;
    private static boolean shutdownOnError = true;

    private static NodeQueueWorker queueWorker;
    private static NodeOperationLog operationLog;
    private static NodeMessageListener messageListener;
    private static NodeStartupUpdator startupUpdator;
    private static NodeFileExchange fileExchange;

    public static String getCurrentNodeId(){
        return nodeId;
    }

    private static void init(){
        nodeId = Options.getStorage().getSystem("cluster.nodeId","node-1");
        shutdownOnError = Options.getStorage().getSystem("cluster.shutdownOnError", true);

        double transactionPriority = Options.getStorage().getSystem("cluster.transactionPriority", 0.5D);
        int workerThreads = Options.getStorage().getSystem("cluster.workerThreads", 10);
        Map<String,Double> priorityTable = Objects.newHashMap();
        List<Map> l = Options.getStorage().getSystem("cluster.priorityTable", Objects.newArrayList(Map.class));
        for(Map<String,Object> m:l){
            String name = Objects.get(String.class, m, "name");
            if(Objects.isNullOrEmpty(name))
                continue;
            priorityTable.put(name, Objects.get(Double.class, m, "priority", 0.5D));
        }
        queueWorker = new NodeQueueWorker(nodeId,workerThreads,transactionPriority,priorityTable);

        String operationLogClass = Options.getStorage().getSystem("cluster.operationLogClass",NodeOperationLog.class.getName());
        try{
            operationLog = (NodeOperationLog)Class.forName(operationLogClass).newInstance();
        }catch (Exception e){
            throw new IllegalArgumentException("Cannot initialize NodeOperationLog ("+operationLogClass+") class",e);
        }

        messageListener = new NodeMessageListener(nodeId,operationLog,queueWorker);
        startupUpdator = new NodeStartupUpdator(nodeId,operationLog);

        int fileThreads = Options.getStorage().getSystem("cluster.fileThreads", 10);
        String fileAddress = Options.getStorage().getSystem("cluster.fileAddress");
        fileExchange = new NodeFileExchange(nodeId,fileThreads,fileAddress);
        initialized = true;
    }

    /**
     * Synchronously start current node
     */
    public static synchronized void start() throws BindException{
        if(status.equals("started"))
            return;
        if(!initialized)
            init();

        long t = System.currentTimeMillis();

        Transactions.clear();
        queueWorker.clear();
        operationLog.initialize();
        messageListener.start();
        //find not
        operationLog.listUndone(new Closure<MessageBean, Object>() {
            @Override
            public Object call(MessageBean input) {
                try{
                    runRealWrite(input);
                    operationLog.markDone(input.getId());
                }catch (Throwable e){
                    onError(input,e);
                }
                return null;
            }
        });
        //update
        startupUpdator.update(queueWorker);
        queueWorker.start();
        startupUpdator.start();
        fileExchange.start();

        status = "started";
        LOG.info("Cluster node "+nodeId+" started ("+(System.currentTimeMillis()-t)+" ms.)");
    }

    /**
     * Synchronously stop current node
     */
    public static synchronized void stop(){
        if(status.equals("stopped") || !initialized)
            return;
        long t = System.currentTimeMillis();

        startupUpdator.stop();
        messageListener.stop();
        fileExchange.stop();
        queueWorker.stop();

        status = "stopped";
        LOG.info("Cluster node "+nodeId+" stopped ("+(System.currentTimeMillis()-t)+" ms.)");
    }

    /**
     * Call distributed data source command
     *
     * @param cls ditributed data source class
     * @param command command name
     * @param params input data
     * @param group string identifies current input data+command, it will be used for lock
     */
    public static void call(Class<? extends DistributedDataSource> cls, String command, Map<String,Object> params, String group){
        MessageBean e = new MessageBean();
        e.setDataSource(cls);
        e.setCommand(command);
        e.setParams(params);
        e.setGroup(group);
        e.setNodeId(nodeId);
        if(Transactions.isInTransaction()){
            Transactions.addOperation(e);
        }else{
            synchronized (ClusterNode.class){
                if(status.equals("stopped") || !initialized)
                    throw new IllegalStateException("Cluster node is stopped");
            }
            long id = messageListener.getNextId();
            e.setId(id);

            messageListener.triggerEvent(e);

            queueWorker.flush(cls,group);

            runCommand(e);
        }
    }

    /**
     * Wait for group is flushed from queue
     *
     * @param cls
     * @param group
     */
    public static void flush(Class<? extends DistributedDataSource> cls, String group){
        queueWorker.flush(cls,group);
    }

    /**
     *
     * @param e
     */
    static void runCommand(MessageBean e){
        try{
            operationLog.addToLocalLog(e);
            runRealWrite(e);
            operationLog.markDone(e.getId());
        }catch (Throwable t){
            onError(e,t);
        }
    }

    /**
     *
     * @param e
     */
    private static void runRealWrite(CommandBean e){
        if(e.getDataSource()!=null){
            runAtomicWrite(e);
        }else{
            //transaction
            if(e.getCommand().equals(Transactions.LIST_COMMAND)){
                List<Map<String,Object>> l = Objects.get(e.getParams(), "list");
                if(l==null)
                    l = Objects.newArrayList();
                for(Map<String,Object> m:l){
                    CommandBean c = new CommandBean();
                    c.fromMap(m);
                    runAtomicWrite(c);
                }
            }
        }
    }

    /**
     *
     * @param e
     */
    private static void runAtomicWrite(CommandBean e){
        DistributedDataSource dds = null;
        try{
            dds = e.getDataSource().newInstance();
        }catch (Throwable ex){
        }
        if(dds!=null){
            dds.runWriteCommand(e.getCommand(),e.getParams());
        }else{
            LOG.warn("Cannot initialize DistributedDataSource with name "+e.getDataSource().getName());
        }
    }

    /**
     *
     * @param e
     * @param err
     */
    private static void onError(MessageBean e, Throwable err){
        LOG.error("Distributed storage error! \n Message: "+e.toString(true),err);
        if(shutdownOnError){
            LOG.info("System shutdown");
            System.exit(1);
        }
    }

}
