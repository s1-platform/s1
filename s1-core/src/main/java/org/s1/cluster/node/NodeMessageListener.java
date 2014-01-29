package org.s1.cluster.node;

import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.s1.cluster.HazelcastWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LOG levels:
 * INFO - started,stopped;
 * DEBUG - every message except params;
 * TRACE - every message with params;
 */
class NodeMessageListener{
    private static final Logger LOG = LoggerFactory.getLogger(NodeMessageListener.class);

    public static final String TOPIC = "s1-node-log";
    public static final String ID_GEN = "s1-node-log-id";

    NodeMessageListener(String nodeId, NodeOperationLog operationLog, NodeQueueWorker queueWorker){
        this.nodeId = nodeId;
        this.operationLog = operationLog;
        this.queueWorker = queueWorker;
    }

    private IAtomicLong idGen;
    private ITopic<MessageBean> topic;
    private String listenerId;
    private String nodeId;
    private NodeOperationLog operationLog;
    private NodeQueueWorker queueWorker;

    private volatile String status = "stopped";

    /**
     *
     */
    synchronized void start(){
        if(status.equals("started"))
            return;
        long t = System.currentTimeMillis();
        topic = HazelcastWrapper.getInstance().getTopic(TOPIC);
        idGen = HazelcastWrapper.getInstance().getAtomicLong(ID_GEN);
        if(HazelcastWrapper.getInstance().getCluster().getMembers().size()==1){
            idGen.set(operationLog.getLastId());
        }

        listenerId = topic.addMessageListener(new MessageListener<MessageBean>() {
            @Override
            public void onMessage(Message<MessageBean> message) {
                MessageBean e = message.getMessageObject();
                if(!nodeId.equals(e.getNodeId())){
                    queueWorker.add(e);
                    if(LOG.isTraceEnabled()){
                        LOG.trace("Write log message recieved: "+e.toString(true));
                    }else if(LOG.isDebugEnabled()){
                        LOG.debug("Write log message recieved: "+e.toString(false));
                    }
                }
            }
        });

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
        topic.removeMessageListener(listenerId);

        status = "stopped";
        LOG.info("Node "+nodeId+" message listener stopped ("+(System.currentTimeMillis()-t)+" ms.), node must be synchronized before next start");
    }

    /**
     *
     * @param e
     */
    void triggerEvent(MessageBean e){
        synchronized (NodeMessageListener.class){
            if(status.equals("stopped")){
                throw new IllegalStateException("Node message listener is stopped, call start() first");
            }
        }
        topic.publish(e);
        if(LOG.isTraceEnabled()){
            LOG.trace("Node " + nodeId + " published new message: "+e.toString(true));
        }else if(LOG.isDebugEnabled()){
            LOG.debug("Node " + nodeId + " published new message: " + e.toString(false));
        }
    }

    /**
     *
     * @return
     */
    long getNextId(){
        synchronized (NodeMessageListener.class){
            if(status.equals("stopped")){
                throw new IllegalStateException("Node message listener is stopped, call start() first");
            }
        }
        return idGen.incrementAndGet();
    }
}
