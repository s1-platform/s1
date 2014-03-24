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

import com.hazelcast.core.IQueue;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.s1.S1SystemError;
import org.s1.cluster.HazelcastWrapper;
import org.s1.cluster.dds.beans.MessageBean;
import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LOG levels:
 * INFO - start, stop, update
 * DEBUG - update requests from new nodes (total)
 * TRACE - update responses with update records
 */
class StartupUpdator {
    private static final Logger LOG = LoggerFactory.getLogger(StartupUpdator.class);

    public static final String TOPIC = "s1-node-log-update-response";
    public static final String QUEUE = "s1-node-log-update-request";

    StartupUpdator(String nodeId, OperationLog operationLog){
        this.nodeId = nodeId;
        this.operationLog = operationLog;
    }

    private String nodeId;
    private OperationLog operationLog;

    private ITopic<UpdateResponseBean> topic;
    private IQueue<UpdateRequestBean> queue;

    private volatile boolean run = false;
    private volatile boolean running = false;
    private volatile String status = "stopped";

    /**
     *
     * @return
     */
    Map<String,Object> getStatistic(){
        Map<String,Object> m = Objects.newHashMap();
        m.put("status",status);
        if("started".equals(status)){
            m.put("watcherRunning",running);
        }
        return m;
    }

    /**
     *
     */
    void update(QueueWorker queueWorker){
        long t = System.currentTimeMillis();

        if(HazelcastWrapper.getInstance().getCluster().getMembers().size()>1){
            long lastId = operationLog.getLastId();
            final List<MessageBean> updateList = Objects.newArrayList();
            final Queue<Object> updateFinished = new ArrayBlockingQueue<Object>(1);
            topic = HazelcastWrapper.getInstance().getTopic(TOPIC);
            queue = HazelcastWrapper.getInstance().getQueue(QUEUE);

            String updateListener = topic.addMessageListener(new MessageListener<UpdateResponseBean>() {
                @Override
                public void onMessage(Message<UpdateResponseBean> message) {
                    UpdateResponseBean r = message.getMessageObject();
                    if(r.getNodeId().equals(nodeId)){
                        if(r.getMessage()==null){
                            updateFinished.add(new Object());
                        }else{
                            updateList.add(r.getMessage());
                            if(LOG.isTraceEnabled())
                                LOG.trace("Node "+nodeId+" have just recieved " +
                                        r.getMessage().toString(true) +
                                        "update record");
                            else if(LOG.isDebugEnabled())
                                LOG.debug("Node " + nodeId + " have just recieved " +
                                        r.getMessage().toString(false) +
                                        "update record");
                        }
                    }
                }
            });

            long toId = lastId;
            try{
                //ask for updates
                queue.add(new UpdateRequestBean(nodeId,lastId));

                //wait for update finished
                while(true){
                    Object stop = updateFinished.poll();
                    if(stop!=null)
                        break;
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        throw S1SystemError.wrap(e);
                    }
                }

                if(updateList.size()>0)
                    toId = updateList.get(updateList.size()-1).getId();
                queueWorker.addFirst(updateList);
            } finally {
                //remove message listener
                topic.removeMessageListener(updateListener);
            }
            LOG.info("Node "+nodeId+" successfully updated ("+(System.currentTimeMillis()-t)+" ms.) from "+lastId+" to "+toId);
        }else{
            LOG.info("Node "+nodeId+" is alone in hazelcast cluster, so nothing to update");
        }
    }

    /**
     *
     */
    synchronized void start(){
        if(status.equals("started"))
            return;
        long t = System.currentTimeMillis();

        topic = HazelcastWrapper.getInstance().getTopic(TOPIC);
        queue = HazelcastWrapper.getInstance().getQueue(QUEUE);

        run = true;
        new Thread(new NewNodeWatcher(),"NewNodeWatcher").start();

        status = "started";
        LOG.info("Node "+nodeId+" message listener started ("+(System.currentTimeMillis()-t)+" ms.) and ready to recieve messages from other nodes");
    }

    /**
     *
     */
    synchronized void stop(){
        if(status.equals("stopped"))
            return;
        long t = System.currentTimeMillis();

        run = false;
        while(running){
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw S1SystemError.wrap(e);
            }
        }

        status = "stopped";
        LOG.info("Node "+nodeId+" message listener stopped ("+(System.currentTimeMillis()-t)+" ms.), node must be synchronized before next start");
    }

    /**
     *
     */
    private class NewNodeWatcher implements Runnable {
        @Override
        public void run() {
            try{
                running = true;
                while(true){
                    if(!run){
                        break;
                    }

                    UpdateRequestBean r1 = null;
                    try {
                        r1 = queue.poll(100, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        throw S1SystemError.wrap(e);
                    }
                    final UpdateRequestBean r = r1;
                    if(r!=null){
                        if(LOG.isDebugEnabled())
                            LOG.debug("Node "+nodeId+" (lastId: "+operationLog.getLastId()+") recieved update request from new node "+r.toString());
                        final AtomicInteger i=new AtomicInteger(0);
                        operationLog.listFrom(r.lastId,new Closure<MessageBean, Object>() {
                            @Override
                            public Object call(MessageBean input) {
                                topic.publish(new UpdateResponseBean(r.getNodeId(),input));
                                if(LOG.isTraceEnabled())
                                    LOG.trace("Node "+nodeId+" have just send " +
                                            "{"+input.toString(true)+"} " +
                                            "update to new node "+r.getNodeId());
                                else if(LOG.isDebugEnabled())
                                    LOG.debug("Node " + nodeId + " have just send " +
                                            "{" + input.toString(false) + "} " +
                                            "update to new node " + r.getNodeId());
                                i.incrementAndGet();
                                return null;
                            }
                        });
                        topic.publish(new UpdateResponseBean(r.getNodeId(),null));
                        if(LOG.isDebugEnabled())
                            LOG.debug("Node "+nodeId+" have just send "+i+" updates to new node "+r.getNodeId());
                    }
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        throw S1SystemError.wrap(e);
                    }
                }
            }finally {
                running = false;
            }
        }
    }

    private static class UpdateRequestBean implements Serializable{
        private String nodeId;
        private long lastId;

        private UpdateRequestBean(String nodeId, long lastId) {
            this.nodeId = nodeId;
            this.lastId = lastId;
        }

        public String getNodeId() {
            return nodeId;
        }

        public long getLastId() {
            return lastId;
        }


        public String toString(){
            return "nodeId: "+nodeId+", lastId: "+lastId;
        }
    }

    private static class UpdateResponseBean implements Serializable{
        private String nodeId;
        private MessageBean message;

        private UpdateResponseBean(String nodeId, MessageBean message) {
            this.nodeId = nodeId;
            this.message = message;
        }

        public String getNodeId() {
            return nodeId;
        }

        public MessageBean getMessage() {
            return message;
        }

        public String toString(){
            return toString(true);
        }

        public String toString(boolean withData){
            return "nodeId: "+nodeId+", message: {"+message.toString(withData)+"}";
        }
    }
}
