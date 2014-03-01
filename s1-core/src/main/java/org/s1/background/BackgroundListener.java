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

    private List<BackgroundWorker> workers = null;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (workers == null) {

            List<Map<String,Object>> l = Options.getStorage().getSystem("backgroundWorkers");
            workers = Objects.newArrayList();
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
                        workers.add(w);
                        w.start();
                    }
                }
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce){
        if(workers!=null){
            for(BackgroundWorker w:workers){
                w.doShutdown();
                w.interrupt();
            }
        }
        workers = null;
    }

}
