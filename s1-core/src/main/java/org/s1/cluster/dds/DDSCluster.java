/*
 * Copyright 2014 Grigory Pykhov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.s1.cluster.dds;

import com.hazelcast.core.IAtomicLong;
import org.s1.S1SystemError;
import org.s1.cluster.HazelcastWrapper;
import org.s1.cluster.dds.beans.MessageBean;
import org.s1.cluster.dds.beans.StorageId;
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
public class DDSCluster {

    private static final Logger LOG = LoggerFactory.getLogger(DDSCluster.class);

    private static String nodeId;
    private static volatile String status = "stopped";
    private static volatile boolean initialized = false;
    private static boolean shutdownOnError = true;

    private static QueueWorker queueWorker;
    private static OperationLog operationLog;
    private static MessageListener messageListener;
    private static StartupUpdator startupUpdator;
    private static FileExchange fileExchange;

    public static final String ID_GEN = "s1-node-log-id";
    private static IAtomicLong idGen;

    public static String getCurrentNodeId(){
        return nodeId;
    }

    protected static long getNextId(){
        synchronized (DDSCluster.class){
            if(status.equals("stopped") || !initialized)
                throw new S1SystemError("Cluster node is stopped");
        }
        return idGen.incrementAndGet();
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

        String operationLogClass = Options.getStorage().getSystem("cluster.operationLogClass",OperationLog.class.getName());
        try{
            operationLog = (OperationLog)Class.forName(operationLogClass).newInstance();
        }catch (Exception e){
            throw new IllegalArgumentException("Cannot initialize NodeOperationLog ("+operationLogClass+") class",e);
        }

        queueWorker = new QueueWorker(nodeId,operationLog,shutdownOnError,workerThreads,transactionPriority,priorityTable);

        idGen = HazelcastWrapper.getInstance().getAtomicLong(ID_GEN);
        if(HazelcastWrapper.getInstance().getCluster().getMembers().size()==1){
            idGen.set(operationLog.getLastId());
        }

        messageListener = new MessageListener(nodeId,queueWorker);
        startupUpdator = new StartupUpdator(nodeId,operationLog);

        int fileThreads = Options.getStorage().getSystem("cluster.fileThreads", 10);
        String fileAddress = Options.getStorage().getSystem("cluster.fileAddress");
        fileExchange = new FileExchange(nodeId,fileThreads,fileAddress);
        initialized = true;
    }

    /**
     * Synchronously start current node
     */
    public static synchronized void start() {
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
                    queueWorker.runRealWrite(input);
                    operationLog.markDone(input.getId());
                }catch (Throwable e){
                    queueWorker.onError(input, e);
                }
                return null;
            }
        });
        //update
        startupUpdator.update(queueWorker);
        queueWorker.start();
        startupUpdator.start();
        try{
            fileExchange.start();
        }catch (BindException e){
            LOG.error("Cannot start file exchange capability, BindException occurs: "+e.getMessage());
            System.exit(1);
        }

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
     *
     * @param e
     */
    public static void call(MessageBean e){
        e.setNodeId(nodeId);

        if(Transactions.isInTransaction()){
            Transactions.addOperation(e);
        }else{
            synchronized (DDSCluster.class){
                if(status.equals("stopped") || !initialized)
                    throw new S1SystemError("Cluster node is stopped");
            }
            long id = getNextId();
            e.setId(id);

            messageListener.triggerEvent(e);

            queueWorker.flush(e);//cls,database,collection,entity);

            queueWorker.runCommand(e);
        }
    }

    /**
     *
     * @param e
     */
    public static void flush(StorageId e){
        queueWorker.flush(e);
    }

}