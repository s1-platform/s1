package org.s1.table;

import java.util.List;

/**
 * Index bean
 */
public class IndexBean {

    private List<String> fields;
    private boolean unique;
    private String uniqueErrorMessage;

    public IndexBean(List<String> fields, boolean unique, String uniqueErrorMessage) {
        this.fields = fields;
        this.unique = unique;
        this.uniqueErrorMessage = uniqueErrorMessage;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public String getUniqueErrorMessage() {
        return uniqueErrorMessage;
    }

    public void setUniqueErrorMessage(String uniqueErrorMessage) {
        this.uniqueErrorMessage = uniqueErrorMessage;
    }
}
