package org.s1.table.format;

import org.s1.objects.Objects;

import java.util.List;
import java.util.Map;

/**
 * Field query node
 */
public class FieldQueryNode extends QueryNode{

    private String field;
    private FieldOperation operation;
    private Object value;

    /**
     *
     */
    public FieldQueryNode() {
    }

    /**
     *
     * @param field
     * @param operation
     * @param value
     */
    public FieldQueryNode(String field, FieldOperation operation, Object value) {
        this.field = field;
        this.operation = operation;
        this.value = value;
    }

    /**
     *
     */
    public static enum FieldOperation{
        EQUALS,NULL,CONTAINS,LTE,LT,GT,GTE
    }

    /**
     *
     * @return
     */
    public Map<String,Object> toMap(){
        Map<String,Object> m = super.toMap();
        m.put("field",field);
        m.put("value",value);
        m.put("operation",operation==null?null:operation.toString().toLowerCase());
        return m;
    }

    /**
     *
     * @param m
     */
    public void fromMap(Map<String,Object> m){
        super.fromMap(m);
        field = Objects.get(String.class,m,"field");
        String o = Objects.get(String.class,m,"operation");
        operation = o==null?null:FieldOperation.valueOf(o.toUpperCase());
        value = Objects.get(m,"value");
    }

    /**
     *
     * @return
     */
    public String getField() {
        return field;
    }

    /**
     *
     * @param field
     */
    public void setField(String field) {
        this.field = field;
    }

    /**
     *
     * @return
     */
    public FieldOperation getOperation() {
        return operation;
    }

    /**
     *
     * @param operation
     */
    public void setOperation(FieldOperation operation) {
        this.operation = operation;
    }

    /**
     *
     * @return
     */
    public Object getValue() {
        return value;
    }

    /**
     *
     * @param value
     */
    public void setValue(Object value) {
        this.value = value;
    }
}
