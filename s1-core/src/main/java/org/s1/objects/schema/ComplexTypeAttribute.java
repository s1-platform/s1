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

import java.util.Map;

/**
 * Complex type attribute
 */
public class ComplexTypeAttribute extends ObjectSchemaAttribute<Map<String,Object>> {

    protected Class<? extends ComplexType> typeClass;
    protected Map<String,Object> typeCfg;

    ComplexTypeAttribute(){
    }

    /**
     *
     * @param name
     * @param label
     * @param typeClass
     */
    public ComplexTypeAttribute(String name, String label, Class<? extends ComplexType> typeClass) {
        super(name,label,"ComplexType");
        this.typeClass = typeClass;
    }

    /**
     *
     * @param name
     * @param label
     * @param typeClass
     * @param typeCfg
     */
    public ComplexTypeAttribute(String name, String label, Class<? extends ComplexType> typeClass, Map<String, Object> typeCfg) {
        this(name,label,typeClass);
        this.typeCfg = Objects.copy(typeCfg);
    }

    @Override
    protected void validateType(boolean expand, boolean deep,  Map<String,Object> ctx, boolean quite) throws Exception{
        if(data!=null && !(data instanceof Map)){
            throw new Exception("Data is not map");
        }

        if(data!=null){
            ComplexType t = typeClass.newInstance();
            t.setConfig(typeCfg);
            data = t.validate(data);
            if(expand)
                data = t.expand(data,deep);
        }
    }

    @Override
    void fromMap(Map<String, Object> m) throws ObjectSchemaFormatException {
        super.fromMap(m);
        this.type = "ComplexType";
        String tc = (String)Objects.get(m,"typeClass");
        try {
            this.typeClass = (Class<? extends ComplexType>)Class.forName(tc);
        } catch (Exception e) {
            throw new ObjectSchemaFormatException("Cannot set property typeClass ("+tc+") to ComplexType ("+getPath(" / ")+")",e);
        }
        this.typeCfg = Objects.newHashMap();
        this.typeCfg = Objects.get(m, "typeCfg", this.typeCfg);
        this.typeCfg = Objects.copy(this.typeCfg);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> m = super.toMap();
        m.put("typeClass",this.typeClass.getName());
        m.put("typeCfg",Objects.copy(this.typeCfg));
        return m;
    }

    /**
     *
     * @return
     */
    public Class<? extends ComplexType> getTypeClass() {
        return typeClass;
    }

    /**
     *
     * @return
     */
    public Map<String, Object> getTypeCfg() {
        return typeCfg;
    }

    /**
     *
     * @param typeClass
     */
    public void setTypeClass(Class<? extends ComplexType> typeClass) {
        this.typeClass = typeClass;
    }

    /**
     *
     * @param typeCfg
     */
    public void setTypeCfg(Map<String, Object> typeCfg) {
        if(typeCfg!=null)
            this.typeCfg = typeCfg;
        else
            this.typeCfg = Objects.newHashMap();
    }
}
