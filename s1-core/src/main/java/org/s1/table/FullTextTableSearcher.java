package org.s1.table;

import java.util.Map;

/**
 * Base class for table full-text capability
 */
public class FullTextTableSearcher {

    private Map<String,Object> config;
    private String name;

    /**
     *
     * @param id
     * @param data
     */
    public void addToIndex(String id, Map<String,Object> data){

    }

    /**
     *
     * @param id
     * @param data
     */
    public void updateIndex(String id, Map<String,Object> data){

    }

    /**
     *
     * @param id
     */
    public void removeFromIndex(String id){

    }

    /**
     *
     * @return
     */
    public Map<String, Object> getConfig() {
        return config;
    }

    /**
     *
     * @param config
     */
    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    /**
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }
}
