package org.s1.table;

import org.s1.objects.schema.ObjectSchema;

/**
 * Table state
 */
public class StateBean {

    private ObjectSchema schema;
    private String name;
    private String label;

    /**
     *
     * @return
     */
    public ObjectSchema getSchema() {
        return schema;
    }

    /**
     *
     * @param schema
     */
    public void setSchema(ObjectSchema schema) {
        this.schema = schema;
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

    /**
     *
     * @return
     */
    public String getLabel() {
        return label;
    }

    /**
     *
     * @param label
     */
    public void setLabel(String label) {
        this.label = label;
    }
}
