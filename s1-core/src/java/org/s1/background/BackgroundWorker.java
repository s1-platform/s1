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

import org.s1.cluster.ClusterLifecycleAction;
import org.s1.lifecycle.LifecycleAction;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;

/**
 * Base class for background workers
 */
public abstract class BackgroundWorker extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(BackgroundWorker.class);

    protected String name = null;
    protected Map<String,Object> config = null;

    /**
     *
     * @return
     */
    public String getWorkerName() {
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
        this.name = name;
        this.config = config;
        this.run = true;
        this.stopped = false;
    }

    private volatile boolean run = true;
    private volatile boolean stopped = false;

    @Override
    public void run() {
        LOG.info(""+name+" ("+this.getClass().getName()+") started with config: {"+config+"}");
        while (true) {
            synchronized (this){
                if(!run)
                    break;
            }
            if(LifecycleAction.isStarted(ClusterLifecycleAction.class)){
                try {
                    MDC.put("id", "");
                    process();
                } catch (Throwable e) {
                    LOG.warn(""+name+" processing error: "+e.getMessage(),e);
                }
            }

            try {
                Thread.sleep(Objects.get(config,"interval",10000L));
            } catch (InterruptedException e) {
                break;
            }
        }
        synchronized (this){
            stopped= true;
        }
    }

    /**
     * Business logic stub
     */
    public abstract void process();

    /**
     * Stopping worker gracefully
     */
    public void doShutdown() {
        long t = System.currentTimeMillis();
        LOG.info(""+name+" is going down now");
        synchronized (this){
            this.stopped = false;
            this.run = false;
        }
        while(true){
            synchronized (this){
                if(stopped)
                    break;
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                break;
            }
        }
        LOG.info(""+name+" stopped in "+(System.currentTimeMillis()-t)+" ms.");
    }

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

    public static void startAll(){
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

    public static void stopAll(){
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