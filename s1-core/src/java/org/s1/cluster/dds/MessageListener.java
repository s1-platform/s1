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

import org.s1.cluster.NodeMessageExchange;
import org.s1.cluster.dds.beans.MessageBean;
import org.s1.misc.Closure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cluster node message listener.
 * <pre>LOG levels:
 * INFO - started,stopped;
 * DEBUG - every message except params;
 * TRACE - every message with params;</pre>
 */
class MessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(MessageListener.class);

    MessageListener(String nodeId, QueueWorker queueWorker){
        this.nodeId = nodeId;
        this.queueWorker = queueWorker;
    }

    private String nodeId;
    private QueueWorker queueWorker;

    private volatile String status = "stopped";

    /**
     *
     */
    synchronized void start(){
        if(status.equals("started"))
            return;
        long t = System.currentTimeMillis();

        NodeMessageExchange.registerOperation("dds.run", new Closure<Object, Object>() {
            @Override
            public Object call(Object input) {
                MessageBean e = (MessageBean)input;
                if(!nodeId.equals(e.getNodeId())){
                    queueWorker.add(e);
                    if(LOG.isTraceEnabled()){
                        LOG.trace("Write log message recieved: "+e.toString(true));
                    }else if(LOG.isDebugEnabled()){
                        LOG.debug("Write log message recieved: "+e.toString(false));
                    }
                }

                return null;
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

        NodeMessageExchange.unregisterOperation("dds.run");

        status = "stopped";
        LOG.info("Node "+nodeId+" message listener stopped ("+(System.currentTimeMillis()-t)+" ms.), node must be synchronized before next start");
    }

    /**
     *
     * @param e
     */
    void triggerEvent(MessageBean e){
        synchronized (MessageListener.class){
            if(status.equals("stopped")){
                throw new IllegalStateException("Node message listener is stopped, call start() first");
            }
        }
        NodeMessageExchange.getInstance().multicastAndForget("dds.run", e);
        //topic.publish(e);
        if(LOG.isTraceEnabled()){
            LOG.trace("Node " + nodeId + " published new message: "+e.toString(true));
        }else if(LOG.isDebugEnabled()){
            LOG.debug("Node " + nodeId + " published new message: " + e.toString(false));
        }
    }

}
