package org.s1.cluster;

import com.hazelcast.core.Hazelcast;
import org.s1.cluster.HazelcastWrapper;
import org.s1.cluster.NodeMessageExchange;
import org.s1.lifecycle.LifecycleAction;
import org.s1.misc.protocols.Init;

/**
 * S1 system action
 */
public class ClusterLifecycleAction extends LifecycleAction {

    private static volatile boolean started = false;

    private static volatile NodeMessageExchange nodeMessageExchange = null;

    /**
     *
     * @return
     */
    public static boolean isStarted(){
        return started;
    }

    /**
     *
     * @return
     */
    public static NodeMessageExchange getNodeMessageExchange(){
        return nodeMessageExchange;
    }

    @Override
    public void start() {
        Init.init();
        HazelcastWrapper.getInstance();
        nodeMessageExchange = new NodeMessageExchange();
        started = true;
    }

    @Override
    public void stop() {
        Hazelcast.shutdownAll();
        started = false;
    }

}
