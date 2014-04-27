/*
 * Copyright 2014 Grigory Pykhov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.s1.table.format;

import org.s1.objects.Objects;

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
