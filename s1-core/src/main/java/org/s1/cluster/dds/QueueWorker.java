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

import org.s1.S1SystemError;
import org.s1.cluster.dds.beans.CommandBean;
import org.s1.cluster.dds.beans.MessageBean;
import org.s1.cluster.dds.beans.StorageId;
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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Node queue worker
 * <pre>
 * INFO - start, stop
 * DEBUG - queue worker started, stopped, message
 * TRACE - message with params</pre>
 */
class QueueWorker {
    private static final Logger LOG = LoggerFactory.getLogger(org.s1.cluster.dds.QueueWorker.class);

    /**
     *
     * @param nodeId
     * @param threads
     * @param transactionPriority
     * @param priorityTable
     */
    QueueWorker(String nodeId, OperationLog operationLog, boolean shutdownOnError, int threads, double transactionPriority, Map<String, Double> priorityTable){
        this.nodeId = nodeId;
        this.operationLog = operationLog;
        this.shutdownOnError = shutdownOnError;
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

    private OperationLog operationLog;
    private boolean shutdownOnError;
    private String nodeId;
    private int threads;
    private double transactionPriority;
    private Map<String,Double> priorityTable;

    private ExecutorService executor;
    private final LinkedList<MessageBean> list = new LinkedList<MessageBean>();
    private final LinkedList<MessageBean> listInWork = new LinkedList<MessageBean>();
    private final Map<String,Map<String,Boolean>> nameThreads = new ConcurrentHashMap<String, Map<String,Boolean>>();

    private volatile boolean run = false;
    private volatile String status = "stopped";

    /**
     *
     * @return
     */
    Map<String,Object> getStatistic(){
        Map<String,Object> m = Objects.newHashMap();
        m.put("status",status);
        if("started".equals(status)){
            m.put("priorityTable",priorityTable);
            m.put("transactionPriority",transactionPriority);
            m.put("threads",threads);
            m.put("queueDepth",list.size());
        }
        return m;
    }

    /**
     *
     */
    synchronized void start(){
        if(status.equals("started"))
            return;
        long t = System.currentTimeMillis();

        synchronized (listInWork){
            listInWork.clear();
        }
        synchronized (nameThreads){
            nameThreads.clear();
        }

        executor = Executors.newFixedThreadPool(threads,new ThreadFactory() {
            private AtomicInteger i = new AtomicInteger(-1);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "QueueWorkerThread-"+i.incrementAndGet());
            }
        });

        run = true;
        int s = 0;
        synchronized (list){
            s = list.size();
        }

        for (int i = 0; i < threads; i++) {
            executor.execute(new QueueWorkerThread());
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
    protected void clear(){
        synchronized (list){
            list.clear();
        }
    }

    /**
     *
     * @param entityId
     */
    protected void flush(final StorageId entityId){
        boolean t = true;
        while(t){
            t = false;
            synchronized (list){
                t = (Objects.find(list,new Closure<MessageBean, Boolean>() {
                    @Override
                    public Boolean call(MessageBean input) {
                        return input.isSameEntity(entityId);
                    }
                })!=null);
            }
            if(!t){
                synchronized (listInWork){
                    t = (Objects.find(listInWork,new Closure<MessageBean, Boolean>() {
                        @Override
                        public Boolean call(MessageBean input) {
                            return input.isSameEntity(entityId);
                        }
                    })!=null);
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
     */
    private class QueueWorkerThread implements Runnable {

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

                String nameOfDDS = null;
                List<MessageBean> arr = Objects.newArrayList();

                synchronized (list){
                    Iterator<MessageBean> it = list.iterator();
                    while(it.hasNext()){
                        MessageBean m = it.next();
                        boolean allowWork = false;
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
                                nameThreads.put(n,Objects.newHashMap(String.class,Boolean.class));
                            int t = nameThreads.get(n).size();
                            allowWork = (t/threads)<w;
                            if(allowWork){
                                nameOfDDS = n;
                                nameThreads.get(nameOfDDS).put(Thread.currentThread().getName(),true);
                            }
                        }

                        if(allowWork){
                            arr.add(m);
                            synchronized (listInWork){
                                listInWork.add(m);
                            }
                            it.remove();
                        }
                    }
                }

                for(MessageBean m:arr){
                    if(LOG.isTraceEnabled()){
                        LOG.trace("Node "+nodeId+" queue worker "+Thread.currentThread().getName()+" processing record: " + m.toString(true));
                    }else if(LOG.isDebugEnabled()){
                        LOG.debug("Node " + nodeId + " queue worker " + Thread.currentThread().getName() + " processing record: " + m.toString(false));
                    }
                    runCommand(m);
                    synchronized (listInWork){
                        listInWork.remove(m);
                    }
                }
                if(nameOfDDS!=null){
                    synchronized (nameThreads){
                        nameThreads.get(nameOfDDS).remove(Thread.currentThread().getName());
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

    /**
     *
     * @param e
     */
    protected void runCommand(MessageBean e){
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
    protected void runRealWrite(CommandBean e){
        if(e.getDataSource()!=null){
            runAtomicWrite(e);
        }else{
            //transaction
            List<CommandBean> l = Objects.get(e.getParams(), "list");
            if(l==null)
                l = Objects.newArrayList();
            for(CommandBean c :l){
                runAtomicWrite(c);
            }
        }
    }

    /**
     *
     * @param e
     */
    private void runAtomicWrite(CommandBean e){
        DistributedDataSource dds = null;
        try{
            dds = e.getDataSource().newInstance();
        }catch (Throwable ex){
        }
        if(dds!=null){
            dds.runWriteCommand(e);
        }else{
            LOG.warn("Cannot initialize DistributedDataSource with name "+e.getDataSource().getName());
        }
    }

    /**
     *
     * @param e
     * @param err
     */
    protected void onError(MessageBean e, Throwable err){
        LOG.error("Distributed storage error! \n Message: "+e.toString(true),err);
        if(shutdownOnError){
            LOG.info("System shutdown");
            System.exit(1);
        }
    }
}
