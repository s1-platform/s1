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

package org.s1.objects.schema;

import org.s1.objects.Objects;

import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 11.01.14
 * Time: 17:17
 */
public class ListAttribute extends ObjectSchemaAttribute<List<Object>> {

    protected List<ObjectSchemaAttribute> list;

    protected int min;
    protected int max;
    protected ObjectSchemaAttribute element;

    ListAttribute(){
    }

    public ListAttribute(String name, String label, ObjectSchemaAttribute element) {
        super(name, label, "List");
        if(element!=null)
            this.element = element;
        else
            this.element = new SimpleTypeAttribute(null,null,Object.class);
        updateChildSchemaAndParent();
    }

    public ListAttribute(String name, String label, ObjectSchemaAttribute element, int min, int max) {
        this(name, label, element);
        this.min = min;
        this.max = max;
    }

    @Override
    public ObjectSchemaAttribute copyAndReset() {
        ListAttribute a = (ListAttribute)super.copyAndReset();
        a.element = element.copyAndReset();
        return a;
    }

    @Override
    protected void validateType(boolean expand, boolean deep, Map<String, Object> ctx, boolean quite) throws Exception {
        if (data != null && !(data instanceof List)) {
            throw new Exception("Data is not list");
        }

        if (data != null) {

            if (min > 0 && data.size() < min) {
                throw new Exception("Shorter than min");
            }
            if (max > 0 && data.size() > max) {
                throw new Exception("Longer than max");
            }

            list = Objects.newArrayList();
            for (int i = 0; i < data.size(); i++) {
                ObjectSchemaAttribute el = element.copyAndReset();
                el.label = "" + i;
                el.name = "" + i;
                el.required = true;
                el.denied = false;
                el.nonPresent = false;
                el.setParent(this);
                el.setSchema(schema);
                list.add(el);
            }
            for (int i = 0; i < data.size(); i++) {
                ObjectSchemaAttribute el = list.get(i);
                ObjectSchemaAttribute va = el.validate(data.get(i), expand, deep, ctx, quite);
                data.set(i, va.getData());
            }
        }
    }

    @Override
    protected void updateChildSchemaAndParent() {
        element.setParent(this);
        element.setSchema(this.schema);
        element.updateChildSchemaAndParent();
        if(list!=null){
            for(ObjectSchemaAttribute a:list){
                a.setSchema(this.schema);
                a.setParent(this);
                a.updateChildSchemaAndParent();
            }
        }
    }

    void fromMap(Map<String, Object> m) throws ObjectSchemaFormatException {
        super.fromMap(m);

        this.min = Objects.cast(Objects.get(m, "min", 0), Integer.class);
        this.max = Objects.cast(Objects.get(m, "max", 0), Integer.class);
        this.element = null;
        Map<String, Object> el = Objects.get(m, "element");
        el = Objects.copy(el);
        if (Objects.isNullOrEmpty(el)) {
            this.element = new SimpleTypeAttribute(null,null,Object.class);
        }else{
            this.element = ObjectSchemaAttribute.createFromMap(el);
            this.element.name = null;
            this.element.label = null;
        }
        updateChildSchemaAndParent();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = super.toMap();
        m.put("min", min);
        m.put("max", max);
        Map<String, Object> el = element.toMap();
        el.remove("name");
        el.remove("label");
        el.remove("appearance");
        m.put("element", el);
        if(list!=null){
            List<Map<String, Object>> attrs = Objects.newArrayList();
            for (ObjectSchemaAttribute a : list) {
                attrs.add(a.toMap());
            }
            m.put("list", attrs);
        }
        return m;
    }

    public List<ObjectSchemaAttribute> getList() {
        return list;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public ObjectSchemaAttribute getElement() {
        return element;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public void setElement(ObjectSchemaAttribute element) {
        if(element!=null)
            this.element = element;
        else
            this.element = new SimpleTypeAttribute(null,null,Object.class);
    }
}
