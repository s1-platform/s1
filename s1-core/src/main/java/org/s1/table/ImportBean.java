package org.s1.table;

import java.util.Map;

/**
 * Input data for import action
 */
public class ImportBean {
    private String id;
    private Map<String,Object> newRecord;
    private Map<String,Object> oldRecord;
    private String state;
    private Map<String,Object> data;

    /**
     *
     * @param id
     * @param newRecord
     * @param oldRecord
     * @param state
     * @param data
     */
    public ImportBean(String id, Map<String, Object> newRecord, Map<String, Object> oldRecord, String state, Map<String, Object> data) {
        this.id = id;
        this.newRecord = newRecord;
        this.oldRecord = oldRecord;
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
    public Map<String, Object> getNewRecord() {
        return newRecord;
    }

    /**
     *
     * @return
     */
    public Map<String, Object> getOldRecord() {
        return oldRecord;
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
