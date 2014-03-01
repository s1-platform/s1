package org.s1.lifecycle;

import java.util.Map;

/**
 * Base class for lifecycle action
 */
public abstract class LifecycleAction {

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
        this.name = name;
        this.config = config;
    }

    /**
     * Business logic stub
     */
    public abstract void start();

    public abstract void stop();

}