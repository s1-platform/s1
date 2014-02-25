package org.s1.table;

import org.s1.objects.Objects;
import org.s1.objects.schema.ObjectSchema;
import org.s1.objects.schema.ObjectSchemaFormatException;

import java.util.Map;

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
    public Map<String,Object> toMap(){
        return Objects.newHashMap(
                "name",name,
                "label",label,
                "schema",schema==null?null:schema.toMap()
        );
    }

    /**
     *
     * @param it
     */
    public void fromMap(Map<String,Object> it) throws ObjectSchemaFormatException{
        this.setName(Objects.get(String.class, it, "name"));
        this.setLabel(Objects.get(String.class, it, "label"));
        Map<String,Object> sm = Objects.get(it,"schema");
        if(!Objects.isNullOrEmpty(sm)){
            ObjectSchema s = new ObjectSchema();
            s.fromMap(sm);
            this.setSchema(s);
        }
    }

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
