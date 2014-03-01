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
import org.s1.lifecycle.LifecycleListener;
import org.s1.objects.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

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
            if(LifecycleListener.isStarted(ClusterLifecycleAction.class)){
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
}