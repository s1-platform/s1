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

package org.s1.cluster.monitor;

import org.s1.cluster.NodeMessageExchange;
import org.s1.lifecycle.LifecycleAction;
import org.s1.log.Loggers;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Monitor lifecycle action
 */
public class MonitorLifecycleAction extends LifecycleAction{

    @Override
    public void start() {
        NodeMessageExchange.registerOperation("monitor.getClusterInfo", new Closure<Serializable, Serializable>() {
            @Override
            public Serializable call(Serializable input) throws ClosureException {
                return (Serializable) MonitorOperation.getStatistic(false);
            }
        });
        NodeMessageExchange.registerOperation("monitor.getNodeInfo",new Closure<Serializable, Serializable>() {
            @Override
            public Serializable call(Serializable input) throws ClosureException {
                return (Serializable)MonitorOperation.getStatistic(true);
            }
        });
        NodeMessageExchange.registerOperation("monitor.getLoggers",new Closure<Serializable, Serializable>() {
            @Override
            public Serializable call(Serializable input) throws ClosureException {
                return (Serializable) Loggers.getLogClasses();
            }
        });
        NodeMessageExchange.registerOperation("monitor.setLogLevel",new Closure<Serializable, Serializable>() {
            @Override
            public Serializable call(Serializable input) throws ClosureException {
                Map<String,Object> m = (Map<String,Object>)input;
                String cls = Objects.get(m,"name");
                String level = Objects.get(m,"level");
                Loggers.setLogLevel(cls,level);
                return true;
            }
        });
        NodeMessageExchange.registerOperation("monitor.listNodeLogs",new Closure<Serializable, Serializable>() {
            @Override
            public Serializable call(Serializable input) throws ClosureException {
                Map<String,Object> m = (Map<String,Object>)input;
                int skip = Objects.get(Integer.class,m,"skip",0);
                int max = Objects.get(Integer.class,m,"max",10);
                if(max>100)
                    max = 100;
                Map<String,Object> search = Objects.get(m,"search");
                List<Map<String,Object>> list = Objects.newArrayList();
                long c = Loggers.getLogStorage().list(list,search,skip,max);
                return (Serializable)Objects.newHashMap("count",c,"list",list);
            }
        });
    }

    @Override
    public void stop() {
        NodeMessageExchange.unregisterOperation("monitor.getClusterInfo");
        NodeMessageExchange.unregisterOperation("monitor.getNodeInfo");
        NodeMessageExchange.unregisterOperation("monitor.getLoggers");
        NodeMessageExchange.unregisterOperation("monitor.setLogLevel");
        NodeMessageExchange.unregisterOperation("monitor.listNodeLogs");
    }
}
