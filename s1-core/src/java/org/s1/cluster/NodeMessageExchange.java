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

package org.s1.cluster;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * Cluster node message exchange
 */
public class NodeMessageExchange {
    private static final Logger LOG = LoggerFactory.getLogger(NodeMessageExchange.class);

    static volatile NodeMessageExchange instance = null;

    public static NodeMessageExchange getInstance(){
        return instance;
    }

    private final String nodeId;
    private final ITopic<NodeMessageBean> topic;
    public static final String TOPIC = "S1NodeExchange";
    private final Map<String,Object> replies = new ConcurrentHashMap<String, Object>();
    public static final int TIMEOUT = 30000;

    private static final Object empty = new Object();

    private static final Map<String,Closure<Object,Object>> operations = new ConcurrentHashMap<String, Closure<Object, Object>>();

    /**
     *
     * @param name
     * @param op
     */
    public static void registerOperation(String name, Closure<Object,Object> op){
        operations.put(name,op);
    }

    /**
     *
     * @param name
     */
    public static void unregisterOperation(String name){
        operations.remove(name);
    }

    public NodeMessageExchange(){
        nodeId = Options.getStorage().getSystem("cluster.nodeId");
        topic = HazelcastWrapper.getInstance().getTopic(TOPIC);
        topic.addMessageListener(new MessageListener<NodeMessageBean>() {
            @Override
            public void onMessage(Message<NodeMessageBean> msg) {
                NodeMessageBean req = msg.getMessageObject();
                if(Objects.newArrayList(NodeMessageBean.ALL,nodeId).contains(req.to)){
                    if(req.operation.equals(NodeMessageBean.REPLY)){
                        if(LOG.isDebugEnabled())
                            LOG.debug("Received node reply <= "+req.toString());
                        //reply
                        synchronized (replies){
                            if(replies.containsKey(req.id)){
                                if(req.from.equals(NodeMessageBean.ALL)){
                                    List<Object> l = (List<Object>)replies.get(req.id);
                                    l.add(req.data);
                                }else{
                                    replies.put(req.id, req.data);
                                }
                            }
                        }
                    }else{
                        //request
                        Object reply = process(req.operation, req.data);
                        topic.publish(new NodeMessageBean(req.id, req.to, req.from, NodeMessageBean.REPLY, reply));
                        if(LOG.isDebugEnabled())
                            LOG.debug("Received node request => "+req.toString());
                    }
                }
            }
        });
        LOG.info("Node message exchange is ready");
    }

    /**
     * Process message
     *
     * @param operation
     * @param data
     * @return
     */
    protected Object process(String operation, Object data){
        Closure<Object,Object> cl = operations.get(operation);
        if(cl!=null){
            return cl.call(data);
        }
        return true;
    }

    /**
     *
     * @param operation
     * @param data
     * @return
     */
    public List<Object> multicast(String operation, Object data) throws TimeoutException {
        int nodes = HazelcastWrapper.getInstance().getCluster().getMembers().size();
        String id = UUID.randomUUID().toString();
        NodeMessageBean m = new NodeMessageBean(id,nodeId,NodeMessageBean.ALL,operation,data);
        try{
            replies.put(id, new ArrayList<Object>());
            topic.publish(m);
            long t = System.currentTimeMillis();
            while(true){
                synchronized (replies){
                    List<Object> l = (List<Object>)replies.get(id);
                    if(l.size()==nodes){
                        return l;
                    }
                }
                if(System.currentTimeMillis()-t>TIMEOUT){
                    throw new TimeoutException("Waiting for reply timeout");
                }

                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }finally {
            synchronized (replies){
                replies.remove(id);
            }
        }
        return null;
    }

    /**
     *
     * @param operation
     * @param data
     */
    public void multicastAndForget(String operation, Object data){
        String id = UUID.randomUUID().toString();
        NodeMessageBean m = new NodeMessageBean(id,nodeId,NodeMessageBean.ALL,operation,data);
        topic.publish(m);
    }

    /**
     *
     * @param to
     * @param operation
     * @param data
     * @return
     */
    public Object request(String to, String operation, Object data) throws TimeoutException {
        String id = UUID.randomUUID().toString();
        NodeMessageBean m = new NodeMessageBean(id,nodeId,to,operation,data);
        try{
            replies.put(id, empty);
            topic.publish(m);
            long t = System.currentTimeMillis();
            while(true){
                synchronized (replies){
                    if(replies.get(id)!=empty){
                        return replies.get(id);
                    }
                }
                if(System.currentTimeMillis()-t>TIMEOUT){
                    throw new TimeoutException("Waiting for reply timeout");
                }

                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }finally {
            synchronized (replies){
                replies.remove(id);
            }
        }
        return null;
    }

    /**
     *
     * @param to
     * @param operation
     * @param data
     */
    public void requestAndForget(String to, String operation, Object data){
        String id = UUID.randomUUID().toString();
        NodeMessageBean m = new NodeMessageBean(id,nodeId,to,operation,data);
        topic.publish(m);
    }

    /**
     *
     */
    public static class NodeRequestBean{
        private String operation;
        private Object data;

        /**
         *
         * @param operation
         * @param data
         */
        public NodeRequestBean(String operation, Object data) {
            this.operation = operation;
            this.data = data;
        }

        /**
         *
         * @return
         */
        public String getOperation() {
            return operation;
        }

        /**
         *
         * @return
         */
        public Object getData() {
            return data;
        }
    }

    /**
     *
     */
    private static class NodeMessageBean implements Serializable{
        public static final String REPLY = "$reply";
        public static final String ALL = "$all";

        private String id;
        private String from;
        private String to;
        private String operation;
        private Object data;

        /**
         *
         * @param from
         * @param to
         * @param operation
         * @param data
         */
        public NodeMessageBean(String id, String from, String to, String operation, Object data) {
            this.id = id;
            this.from = from;
            this.to = to;
            this.operation = operation;
            this.data = data;
        }

        @Override
        public String toString() {
            return "id: "+id+", from: "+from+", to: "+to+", operation: "+operation+", \n\tdata: "+data;
        }
    }

}
