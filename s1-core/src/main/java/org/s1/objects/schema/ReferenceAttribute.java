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

import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.s1.objects.schema.errors.ObjectSchemaFormatException;

import java.util.List;
import java.util.Map;

/**
 * Reference attribute
 */
public class ReferenceAttribute extends ObjectSchemaAttribute<Map<String,Object>> {

    ReferenceAttribute(){
    }

    /**
     *
     * @param name
     * @param label
     * @param type
     */
    public ReferenceAttribute(String name, String label, String type) {
        super(name,label,type);
    }

    void fromMap(Map<String,Object> m) throws ObjectSchemaFormatException {
        super.fromMap(m);
        if(!this.type.startsWith("#"))
            throw new ObjectSchemaFormatException("Reference type attribute ("+getPath(" / ")+") must starts with #");
        this.type = this.type.substring(1);
    }

    public Map<String,Object> toMap(){
        Map<String,Object> m = super.toMap();
        m.put("type","#"+m.get("type"));
        return m;
    }

    MapAttribute resolve() throws ObjectSchemaFormatException{
        ObjectSchemaType t = Objects.find(schema.getTypes(), new Closure<ObjectSchemaType, Boolean>() {
            @Override
            public Boolean call(ObjectSchemaType input) {
                return input.getName().equals(type);
            }
        });
        if(t==null)
            throw new ObjectSchemaFormatException("reference not found");

        List<ObjectSchemaAttribute> list = Objects.newArrayList();
        for(ObjectSchemaAttribute a:t.getAttributes()){
            list.add(a.copyAndReset());
        }
        MapAttribute a = new MapAttribute(name,label,list);
        a.setData(data);
        a.setSchema(schema);
        a.setParent(parent);
        return a;
    }

    protected void validateType(boolean expand, boolean deep,  Map<String,Object> ctx, boolean quite) throws Exception{
    }

}
