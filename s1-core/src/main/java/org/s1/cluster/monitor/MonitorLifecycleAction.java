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
