package org.s1.table;

import java.util.Map;

/**
 * Input data for enrich closure
 */
public class EnrichBean {
    private Map<String,Object> record;
    private Map<String,Object> context;
    private boolean list;

    /**
     *
     * @param record
     * @param context
     * @param list
     */
    public EnrichBean(Map<String, Object> record, Map<String, Object> context, boolean list) {
        this.record = record;
        this.context = context;
        this.list = list;
    }

    /**
     *
     * @return
     */
    public Map<String, Object> getRecord() {
        return record;
    }

    /**
     *
     * @return
     */
    public Map<String, Object> getContext() {
        return context;
    }

    /**
     *
     * @return
     */
    public boolean isList() {
        return list;
    }
}
