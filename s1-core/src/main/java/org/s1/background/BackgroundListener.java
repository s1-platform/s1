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
 * Background servlet listener. Workers list is described in system options on path 'backgroundWorkers'
 * <pre>backgroundWorkers = [{
 *     name:"... default is Worker#i ...",
 *     class:"...subclass of {@link org.s1.background.BackgroundWorker} ...",
 *     config:{...configuration...}
 * },...]</pre>
 */
public class BackgroundListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(BackgroundListener.class);

    private static List<BackgroundWorker> workers = Objects.newArrayList();

    /**
     *
     * @param cls
     * @return
     */
    public static boolean isStarted(Class<? extends BackgroundWorker> cls){
        synchronized (workers){
            boolean b = false;
            for(BackgroundWorker a:workers){
                if(a.getClass()==cls){
                    b = true;
                    break;
                }
            }
            return b;
        }
    }

    /**
     *
     * @param name
     * @return
     */
    public static boolean isStarted(String name){
        synchronized (workers){
            boolean b = false;
            for(BackgroundWorker a:workers){
                if(a.getName().equals(name)){
                    b = true;
                    break;
                }
            }
            return b;
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        synchronized (workers){
            workers.clear();
        }

        List<Map<String,Object>> l = Options.getStorage().getSystem("backgroundWorkers");
        List<BackgroundWorker> lst = Objects.newArrayList();
        if(l!=null){
            int i=0;
            for(Map<String,Object> m : l){
                String cls = Objects.get(m,"class");
                String name = Objects.get(m,"name","BackgroundWorker#"+i);
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
                    lst.add(w);
                    try{
                        w.start();
                    }catch (Throwable e){
                        LOG.warn("Worker #"+i+" failed to start, "+e.getClass().getName()+": "+e.getMessage(),e);
                    }
                }
            }
        }
        synchronized (workers){
            workers.addAll(lst);
        }
        LOG.info("Background started");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce){
        List<BackgroundWorker> l = Objects.newArrayList();
        synchronized (workers){
            l.addAll(workers);
        }
        for(BackgroundWorker w:l){
            w.doShutdown();
            w.interrupt();
        }
        synchronized (workers){
            workers.clear();
        }
        LOG.info("Background stopped");
    }

}
