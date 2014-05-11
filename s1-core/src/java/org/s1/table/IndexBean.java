package org.s1.table;

import java.util.List;

/**
 * Index bean
 */
public class IndexBean {

    private List<String> fields;

    public IndexBean(List<String> fields) {
        this.fields = fields;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

}
