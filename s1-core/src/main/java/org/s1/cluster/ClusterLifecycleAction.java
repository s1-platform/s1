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

import com.hazelcast.core.Hazelcast;
import org.s1.cluster.dds.DDSCluster;
import org.s1.lifecycle.LifecycleAction;
import org.s1.misc.protocols.Init;

/**
 * S1 system action
 */
public class ClusterLifecycleAction extends LifecycleAction {

    @Override
    public void start() {
        Init.init();
        HazelcastWrapper.getInstance();
        NodeMessageExchange.instance = new NodeMessageExchange();
        DDSCluster.start();
    }

    @Override
    public void stop() {
        DDSCluster.stop();
        Hazelcast.shutdownAll();
        NodeMessageExchange.instance = null;
    }

}
