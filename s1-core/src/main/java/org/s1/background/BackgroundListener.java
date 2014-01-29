package org.s1.background;

import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 11.01.14
 * Time: 13:03
 */
public class BackgroundListener implements ServletContextListener {
    public static final Logger LOG = LoggerFactory.getLogger(BackgroundListener.class);
    private Map<String,BackgroundWorker> workers = null;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (workers == null) {
            List<Map<String,Object>> l = Options.getStorage().getSystem("backgroundWorkers");
            workers = Objects.newHashMap();
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

    @Override
    public void contextDestroyed(ServletContextEvent sce){
        if(workers!=null){
            for(String name:workers.keySet()){
                workers.get(name).doShutdown();
                workers.get(name).interrupt();
            }
        }
        workers = null;
    }

}
