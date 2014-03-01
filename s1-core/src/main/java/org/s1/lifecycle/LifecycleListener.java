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

package org.s1.lifecycle;

import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.List;
import java.util.Map;

/**
 * Lifecycle servlet listener. Actions list is described in system options on path 'lifecycleActions'
 * <pre>lifecycleActions = [{
 *     name:"... default is LifecycleAction#i ...",
 *     class:"...subclass of {@link org.s1.lifecycle.LifecycleAction} ...",
 *     config:{...configuration...}
 * },...]</pre>
 */
public class LifecycleListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(LifecycleListener.class);

    private List<LifecycleAction> actions = null;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (actions == null) {

            List<Map<String,Object>> l = Options.getStorage().getSystem("lifecycleActions");
            actions = Objects.newArrayList();
            if(l!=null){
                int i=0;
                for(Map<String,Object> m : l){
                    String cls = Objects.get(m,"class");
                    String name = Objects.get(m,"name","LifecycleAction#"+i);
                    Map<String,Object> cfg = Objects.get(m,"config",Objects.newHashMap(String.class,Object.class));
                    i++;
                    LifecycleAction w = null;
                    try{
                        w = (LifecycleAction)Class.forName(cls).newInstance();
                        w.init(name,cfg);
                    }catch (Throwable e){
                        LOG.warn("Cannot initialize action "+name+" ("+cls+")");
                    }
                    if(w!=null){
                        actions.add(w);
                        try{
                            w.start();
                        }catch (Throwable e){
                            LOG.warn("Action #"+i+" failed to start, "+e.getClass().getName()+": "+e.getMessage(),e);
                        }
                    }
                }
            }
        }
        LOG.info("S1 started");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce){
        if(actions!=null){
            for(int i=actions.size()-1;i>=0;i--){
                try{
                    actions.get(i).stop();
                }catch (Throwable e){
                    LOG.warn("Action #"+i+" failed to stop, "+e.getClass().getName()+": "+e.getMessage(),e);
                }
            }
        }
        actions = null;

        //stop
        LOG.info("S1 stopped");
    }

}
