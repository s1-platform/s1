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
import org.s1.script.S1ScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Base class for lifecycle action
 */
public abstract class LifecycleAction {

    private static final Logger LOG = LoggerFactory.getLogger(LifecycleAction.class);

    protected String name = null;
    protected Map<String,Object> config = null;

    /**
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return
     */
    public Map<String, Object> getConfig() {
        return config;
    }

    /**
     * Initializing with name and config
     *
     * @param name
     * @param config
     */
    public void init(String name, Map<String,Object> config) {
        if(config==null)
            config = Objects.newSOHashMap();
        this.name = name;
        this.config = config;
    }

    /**
     * Business logic stub
     */
    public abstract void start();

    public abstract void stop();

    private static final List<LifecycleAction> actions = Objects.newArrayList();

    /**
     *
     * @param cls
     * @return
     */
    public static boolean isStarted(Class<? extends LifecycleAction> cls){
        synchronized (actions){
            boolean b = false;
            for(LifecycleAction a:actions){
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
        synchronized (actions){
            boolean b = false;
            for(LifecycleAction a:actions){
                if(a.getName().equals(name)){
                    b = true;
                    break;
                }
            }
            return b;
        }
    }

    public static void startAll(){
        synchronized (actions){
            actions.clear();
        }

        List<Map<String,Object>> l = Options.getStorage().getSystem("lifecycleActions");
        List<LifecycleAction> lst = Objects.newArrayList();
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
                    lst.add(w);
                    try{
                        w.start();
                    }catch (Throwable e){
                        LOG.warn("Action #"+i+" failed to start, "+e.getClass().getName()+": "+e.getMessage(),e);
                    }
                }
            }
        }
        synchronized (actions){
            actions.addAll(lst);
        }
        LOG.info("S1 started");
    }

    public static void stopAll(){
        List<LifecycleAction> l = Objects.newArrayList();
        synchronized (actions){
            l.addAll(actions);
        }
        for(int i=l.size()-1;i>=0;i--){
            try{
                l.get(i).stop();
            }catch (Throwable e){
                LOG.warn("Action #"+i+" failed to stop, "+e.getClass().getName()+": "+e.getMessage(),e);
            }
        }
        synchronized (actions){
            actions.clear();
        }

        //stop s1 script
        S1ScriptEngine.stopAll();

        //stop
        LOG.info("S1 stopped");
    }
}