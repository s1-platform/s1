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

    private final String nodeId;
    private final ITopic<NodeMessageBean> topic;
    public static final String TOPIC = "S1NodeExchange";
    private final Map<String,Serializable> replies = new ConcurrentHashMap<String, Serializable>();
    public static final int TIMEOUT = 30000;

    private static final Map<String,Closure<Serializable,Serializable>> operations = new ConcurrentHashMap<String, Closure<Serializable, Serializable>>();

    /**
     *
     * @param name
     * @param op
     */
    public static void registerOperation(String name, Closure<Serializable,Serializable> op){
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
                                if(req.operation.equals(NodeMessageBean.ALL)){
                                    List<Serializable> l = (List<Serializable>)replies.get(req.id);
                                    l.add(req.data);
                                }else{
                                    replies.put(req.id, req.data);
                                }
                            }
                        }
                    }else{
                        //request
                        Serializable reply = process(req.operation, req.data);
                        topic.publish(new NodeMessageBean(req.id, nodeId, req.from, NodeMessageBean.REPLY, reply));
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
    protected Serializable process(String operation, Serializable data){
        Closure<Serializable,Serializable> cl = operations.get(operation);
        if(cl!=null){
            return cl.callQuite(data);
        }
        return true;
    }

    /**
     *
     * @param operation
     * @param data
     * @return
     */
    public List<Serializable> multicast(String operation, Serializable data) throws TimeoutException {
        int nodes = HazelcastWrapper.getInstance().getCluster().getMembers().size();
        String id = UUID.randomUUID().toString();
        NodeMessageBean m = new NodeMessageBean(id,nodeId,NodeMessageBean.ALL,operation,data);
        try{
            replies.put(id, new ArrayList<Serializable>());
            topic.publish(m);
            long t = System.currentTimeMillis();
            while(true){
                synchronized (replies){
                    List<Serializable> l = (List<Serializable>)replies.get(id);
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
    public void multicastAndForget(String operation, Serializable data){
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
    public Serializable request(String to, String operation, Serializable data) throws TimeoutException {
        String id = UUID.randomUUID().toString();
        NodeMessageBean m = new NodeMessageBean(id,nodeId,to,operation,data);
        try{
            replies.put(id, null);
            topic.publish(m);
            long t = System.currentTimeMillis();
            while(true){
                synchronized (replies){
                    if(replies.get(id)!=null){
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
    public void requestAndForget(String to, String operation, Serializable data){
        String id = UUID.randomUUID().toString();
        NodeMessageBean m = new NodeMessageBean(id,nodeId,to,operation,data);
        topic.publish(m);
    }

    /**
     *
     */
    public static class NodeRequestBean{
        private String operation;
        private Serializable data;

        /**
         *
         * @param operation
         * @param data
         */
        public NodeRequestBean(String operation, Serializable data) {
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
        public Serializable getData() {
            return data;
        }
    }

    /**
     *
     */
    private static class NodeMessageBean{
        public static final String REPLY = "$reply";
        public static final String ALL = "$all";

        private String id;
        private String from;
        private String to;
        private String operation;
        private Serializable data;

        /**
         *
         * @param from
         * @param to
         * @param operation
         * @param data
         */
        public NodeMessageBean(String id, String from, String to, String operation, Serializable data) {
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
