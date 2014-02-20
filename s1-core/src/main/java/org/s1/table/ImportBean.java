package org.s1.table;

import java.util.Map;

/**
 * Input data for import action
 */
public class ImportBean {
    private String id;
    private Map<String,Object> newObject;
    private Map<String,Object> oldObject;
    private String state;
    private Map<String,Object> data;

    /**
     *
     * @param id
     * @param newObject
     * @param oldObject
     * @param state
     * @param data
     */
    public ImportBean(String id, Map<String, Object> newObject, Map<String, Object> oldObject, String state, Map<String, Object> data) {
        this.id = id;
        this.newObject = newObject;
        this.oldObject = oldObject;
        this.state = state;
        this.data = data;
    }

    /**
     *
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     *
     * @return
     */
    public Map<String, Object> getNewObject() {
        return newObject;
    }

    /**
     *
     * @return
     */
    public Map<String, Object> getOldObject() {
        return oldObject;
    }

    /**
     *
     * @return
     */
    public String getState() {
        return state;
    }

    /**
     *
     * @return
     */
    public Map<String, Object> getData() {
        return data;
    }
}
