package org.s1.background;

import com.hazelcast.core.Hazelcast;
import org.s1.S1SystemError;
import org.s1.cluster.node.ClusterNode;
import org.s1.misc.protocols.Init;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.List;
import java.util.Map;

/**
 * Background servlet listener. Workers list is described in system options on path 'backgroundWorkers'
 * <pre>backgroundWorkers = [{
 *     name:"... default is Worker#i ...",
 *     class:"...subclass of {@link org.s1.background.BackgroundWorker} ...",
 *     config:{...configuration...}
 * },...]</pre>
 */
public class BackgroundListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(BackgroundListener.class);

    private Map<String,BackgroundWorker> workers = null;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        //start cluster node
        Init.init();
        try{
            ClusterNode.start();
        }catch (Exception e){
            LOG.error("Cannot start ClusterNode: "+e.getMessage(),e);
            throw S1SystemError.wrap(e);
        }

        if (workers == null) {

            List<Map<String,Object>> l = Options.getStorage().getSystem("backgroundWorkers");
            workers = Objects.newHashMap();
            if(l!=null){
                int i=0;
                for(Map<String,Object> m : l){
                    String cls = Objects.get(m,"class");
                    String name = Objects.get(m,"name","Worker#"+i);
                    Map<String,Object> cfg = Objects.get(m,"config",Objects.newHashMap(String.class,Object.class));
                    i++;
                    BackgroundWorker w = null;
                    try{
                        w = (BackgroundWorker)Class.forName(cls).newInstance();
                        w.init(name,cfg);
                    }catch (Throwable e){
                        LOG.warn("Cannot initialize worker "+name+" ("+cls+")");
                    }
                    if(w!=null){
                        workers.put(name,w);
                        w.start();
                    }
                }
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce){
        if(workers!=null){
            for(String name:workers.keySet()){
                workers.get(name).doShutdown();
                workers.get(name).interrupt();
            }
        }
        workers = null;

        //stop
        ClusterNode.stop();
        Hazelcast.shutdownAll();
    }

}
