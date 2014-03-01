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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Schema type for references
 */
public class ObjectSchemaType {

    private String name;

    private List<ObjectSchemaAttribute> attributes = Objects.newArrayList();

    ObjectSchemaType(Map<String, Object> m) throws ObjectSchemaFormatException {
        fromMap(m);
    }

    /**
     *
     * @param name
     * @param attributes
     */
    public ObjectSchemaType(String name, List<ObjectSchemaAttribute> attributes) {
        this.name = name;
        if(attributes!=null)
            this.attributes = attributes;
    }

    /**
     *
     * @param name
     * @param args
     */
    public ObjectSchemaType(String name, ObjectSchemaAttribute... args) {
        this.name = name;
        this.attributes = Arrays.asList(args);
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
     * @return
     */
    public List<ObjectSchemaAttribute> getAttributes() {
        List<ObjectSchemaAttribute> list = Objects.newArrayList();
        for(ObjectSchemaAttribute a:attributes) list.add(a);
        return list;
    }

    void fromMap(Map<String,Object> m) throws ObjectSchemaFormatException{
        this.name = Objects.get(m, "name");
        List<Map<String,Object>> attrs = Objects.get(m, "attributes");
        this.attributes = Objects.newArrayList();
        if(!Objects.isNullOrEmpty(attrs)){
            for(Map<String,Object> a: attrs){
                attributes.add(ObjectSchemaAttribute.createFromMap(a));
            }
        }
    }

    /**
     *
     * @return
     */
    public Map<String,Object> toMap(){
        List<Map<String,Object>> attrs = Objects.newArrayList();
        for(ObjectSchemaAttribute a:attributes){
            attrs.add(a.toMap());
        }
        Map<String,Object> m = Objects.newHashMap("name", name, "attributes", attrs);
        return m;
    }

    /**
     *
     * @return
     */
    public ObjectSchemaType copy(){
        ObjectSchemaType a = new ObjectSchemaType(name);
        a.attributes = Objects.newArrayList();
        for(ObjectSchemaAttribute a1:attributes){
            a.attributes.add(a1.copyAndReset());
        }
        return a;
    }

}
