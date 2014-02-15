package org.s1.cluster.node;

import com.hazelcast.core.ITopic;
import org.s1.S1SystemError;
import org.s1.cluster.HazelcastWrapper;
import org.s1.cluster.datasource.DistributedDataSource;
import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Node queue worker
 * <pre>
 * INFO - start, stop
 * DEBUG - queue worker started, stopped, message
 * TRACE - message with params</pre>
 */
class NodeQueueWorker {
    private static final Logger LOG = LoggerFactory.getLogger(NodeQueueWorker.class);

    /**
     *
     * @param nodeId
     * @param threads
     * @param transactionPriority
     * @param priorityTable
     */
    NodeQueueWorker(String nodeId, int threads, double transactionPriority, Map<String,Double> priorityTable){
        this.nodeId = nodeId;
        if(threads>0)
            this.threads = threads;
        else
            this.threads = 10;
        if(priorityTable==null)
            priorityTable = Objects.newHashMap();
        for(String k:priorityTable.keySet()){
            if(priorityTable.get(k)<=0||priorityTable.get(k)>1)
                priorityTable.put(k,0.5D);
        }
        this.priorityTable = priorityTable;

        if(transactionPriority>0 && transactionPriority<=1)
            this.transactionPriority = transactionPriority;
        else
            this.transactionPriority = 0.5D;
    }

    private String nodeId;
    private int threads;
    private double transactionPriority;
    private Map<String,Double> priorityTable;

    private ITopic topic;
    private ExecutorService executor;
    private final LinkedList<MessageBean> list = new LinkedList<MessageBean>();
    private final LinkedList<String> workingGroups = new LinkedList<String>();
    private final Map<String,Integer> nameThreads = new ConcurrentHashMap<String, Integer>();

    private volatile boolean run = false;
    private volatile String status = "stopped";

    /**
     *
     */
    synchronized void start(){
        if(status.equals("started"))
            return;
        long t = System.currentTimeMillis();
        topic = HazelcastWrapper.getInstance().getTopic(NodeMessageListener.TOPIC);

        synchronized (workingGroups){
            workingGroups.clear();
        }
        synchronized (nameThreads){
            nameThreads.clear();
        }

        executor = Executors.newFixedThreadPool(threads);

        run = true;
        int s = 0;
        synchronized (list){
            s = list.size();
        }

        for (int i = 0; i < threads; i++) {
            executor.execute(new QueueWorker());
        }

        status = "started";
        LOG.info("Node "+nodeId+" queue worker started ("+(System.currentTimeMillis()-t)+" ms.) with "+threads+" threads, write log messages in queue: "+s+", processing them...");
    }

    /**
     *
     */
    synchronized void stop(){
        if(status.equals("stopped"))
            return;
        long t = System.currentTimeMillis();
        run = false;

        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        status = "stopped";
        int s = 0;
        synchronized (list){
            s = list.size();
        }
        LOG.info("Node "+nodeId+" queue worker stopped ("+(System.currentTimeMillis()-t)+" ms.), "+s+" log messages left");
    }

    /**
     *
     * @param m
     */
    void add(final MessageBean m){
        synchronized (list){
            if(Objects.find(list,new Closure<MessageBean, Boolean>() {
                @Override
                public Boolean call(MessageBean input) {
                    return input.getId()==m.getId();
                }
            })==null)
                list.add(m);
        }
    }

    /**
     *
     * @param l
     */
    void addFirst(List<MessageBean> l){
        synchronized (list){
            Iterator<MessageBean> it = l.iterator();
            while(it.hasNext()){
                final MessageBean m = it.next();
                if(Objects.find(list,new Closure<MessageBean, Boolean>() {
                    @Override
                    public Boolean call(MessageBean input) {
                        return input.getId()==m.getId();
                    }
                })!=null)
                    it.remove();
            }
            list.addAll(0,l);
        }
    }

    /**
     *
     */
    void clear(){
        synchronized (list){
            list.clear();
        }
    }

    /**
     *
     * @param cls
     * @param group
     */
    void flush(final Class<? extends DistributedDataSource> cls, final String group){
        boolean t = true;
        while(t){
            t = false;
            synchronized (list){
                t = (Objects.find(list,new Closure<MessageBean, Boolean>() {
                    @Override
                    public Boolean call(MessageBean input) {
                        return input.getDataSource()!=null && input.getDataSource().getName().equals(cls.getName())
                                && input.getGroup().equals(group);
                    }
                })!=null);
            }
            if(!t){
                synchronized (workingGroups){
                    t = workingGroups.contains(getUniqueNameGroupId(cls,group));
                }
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw S1SystemError.wrap(e);
            }
        }
    }

    /**
     *
     * @param name
     * @param group
     * @return
     */
    private String getUniqueNameGroupId(Class<? extends DistributedDataSource> name, String group){
        return (name!=null?name.getName():null)+":::"+group;
    }

    /**
     *
     */
    private class QueueWorker implements Runnable {

        @Override
        public void run() {
            if(LOG.isDebugEnabled())
                LOG.debug("Node "+nodeId+" queue worker "+Thread.currentThread().getName()+" started");
            while(true){
                //check queue size
                int list_size = 0;
                synchronized (list){
                    list_size = list.size();
                }
                if(list_size==0){
                    if(!run){
                        break;
                    }
                }

                String name = null;
                String group = null;
                List<MessageBean> arr = Objects.newArrayList();

                synchronized (list){
                    Iterator<MessageBean> it = list.iterator();
                    while(it.hasNext()){
                        MessageBean m = it.next();
                        boolean b = false;
                        synchronized (nameThreads){
                            String n = m.getDataSource()==null?null:m.getDataSource().getName();
                            double w = 0.5D;
                            if(n==null){
                                n = "transaction";
                                w = transactionPriority;
                            }else{
                                if(priorityTable.containsKey(n))
                                    w = priorityTable.get(n);
                            }
                            if(!nameThreads.containsKey(n))
                                nameThreads.put(n,0);
                            int t = nameThreads.get(n);
                            b = (t/threads)<w;
                            if(b && group==null){
                                name = n;
                                nameThreads.put(name,t+1);
                            }
                        }

                        if(b){
                            if(group==null){
                                group = getUniqueNameGroupId(m.getDataSource(),m.getGroup());
                                arr.add(m);
                                synchronized (workingGroups){
                                    workingGroups.add(group);
                                }
                                it.remove();
                            }else{
                                if(getUniqueNameGroupId(m.getDataSource(),m.getGroup()).equals(group)){
                                    arr.add(m);
                                    it.remove();
                                }
                            }
                        }
                    }
                }

                for(MessageBean m:arr){
                    if(LOG.isTraceEnabled()){
                        LOG.trace("Node "+nodeId+" queue worker "+Thread.currentThread().getName()+" processing record: " + m.toString(true));
                    }else if(LOG.isDebugEnabled()){
                        LOG.debug("Node " + nodeId + " queue worker " + Thread.currentThread().getName() + " processing record: " + m.toString(false));
                    }
                    ClusterNode.runCommand(m);
                }
                synchronized (workingGroups){
                    workingGroups.remove(group);
                }
                if(name!=null){
                    synchronized (nameThreads){
                        nameThreads.put(name,nameThreads.get(name)-1);
                    }
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw S1SystemError.wrap(e);
                }
            }
            if(LOG.isDebugEnabled())
                LOG.debug("Node "+nodeId+" queue worker "+Thread.currentThread().getName()+" stopped");
        }
    }
}
