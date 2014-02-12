package org.s1.background;

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
            try {
                MDC.put("id", "");
                process();
            } catch (Throwable e) {
                LOG.warn(""+name+" processing error: "+e.getMessage(),e);
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