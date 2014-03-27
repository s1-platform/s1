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

import org.s1.S1SystemError;
import org.s1.objects.Objects;
import org.s1.objects.schema.errors.ObjectSchemaFormatException;
import org.s1.objects.schema.errors.ValidationException;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 11.01.14
 * Time: 17:16
 */
public class ObjectSchema implements Serializable{

    public static enum Mode{
        STRICT,LAX
    }

    private Mode mode;

    public Mode getMode() {
        return mode;
    }

    private boolean resolved = false;

    private List<ObjectSchemaAttribute> attributes = Objects.newArrayList();

    private List<ObjectSchemaType> types = Objects.newArrayList();

    public ObjectSchema(Object... args) {
        this(Mode.STRICT, args);
    }

    public ObjectSchema(Mode mode, Object... args) {
        this.mode = mode;
        this.attributes = Objects.newArrayList();
        this.types = Objects.newArrayList();
        for(Object a:args){
            if(a instanceof ObjectSchemaAttribute){
                this.attributes.add((ObjectSchemaAttribute)a);
            }else if(a instanceof ObjectSchemaType){
                this.types.add((ObjectSchemaType)a);
            }
        }
        setAttributesSchema();
    }

    public ObjectSchema(List<ObjectSchemaAttribute> attributes, List<ObjectSchemaType> types) {
        this(Mode.STRICT, attributes, types);
    }

    public ObjectSchema(Mode mode,List<ObjectSchemaAttribute> attributes, List<ObjectSchemaType> types) {
        this.mode = mode;
        if(attributes==null)
            this.attributes = Objects.newArrayList();
        else
            this.attributes = attributes;
        if(types==null)
            this.types = Objects.newArrayList();
        else
            this.types = types;
        setAttributesSchema();
    }

    public ObjectSchema fromMap(Map<String,Object> m) throws ObjectSchemaFormatException {
        this.mode = Mode.valueOf(Objects.get(m,"mode","strict").toUpperCase());
        List<Map<String,Object>> attrs = Objects.get(m, "attributes");
        this.attributes = Objects.newArrayList();
        if(!Objects.isNullOrEmpty(attrs)){
            for(Map<String,Object> a: attrs){
                attributes.add(ObjectSchemaAttribute.createFromMap(a));
            }
        }

        List<Map<String,Object>> t = Objects.get(m, "types");
        this.types = Objects.newArrayList();
        if(!Objects.isNullOrEmpty(t)){
            for(Map<String,Object> a: t){
                types.add(new ObjectSchemaType(a));
            }
        }
        setAttributesSchema();
        return this;
    }

    protected MapAttribute rootMapAttribute = new MapAttribute(null,null);

    /**
     *
     * @return
     */
    public MapAttribute getRootMapAttribute(){
        return rootMapAttribute;
    }

    protected void setAttributesSchema(){
        rootMapAttribute.attributes = attributes;
        rootMapAttribute.setSchema(this);
        /*for(ObjectSchemaAttribute a:attributes){
            a.setSchema(this);
            a.setParent(null);
        }*/
    }

    public Map<String,Object> toMap(){
        List<Map<String,Object>> attrs = Objects.newArrayList();
        for(ObjectSchemaAttribute a:attributes){
            attrs.add(a.toMap());
        }

        List<Map<String,Object>> t = Objects.newArrayList();
        for(ObjectSchemaType a:types){
            t.add(a.toMap());
        }

        Map<String,Object> m = Objects.newHashMap("attributes", attrs, "types", t, "mode", mode.toString().toLowerCase());
        return m;
    }

    public List<ObjectSchemaAttribute> getAttributes() {
        List<ObjectSchemaAttribute> list = Objects.newArrayList();
        for(ObjectSchemaAttribute a:attributes) list.add(a);
        return list;
    }

    public List<ObjectSchemaType> getTypes() {
        List<ObjectSchemaType> list = Objects.newArrayList();
        for(ObjectSchemaType a:types) list.add(a);
        return list;
    }

    public boolean isResolved() {
        return resolved;
    }

    public ValidateResultBean validateQuite(Map<String,Object> data){
        return validateQuite(data,null);
    }

    public ValidateResultBean validateQuite(Map<String,Object> data, Map<String,Object> ctx){
        return validateQuite(data, false, false, ctx);
    }

    public ValidateResultBean validateQuite(Map<String,Object> data, boolean expand, boolean deep, Map<String,Object> ctx){
        try {
            return validate(data, expand, deep, ctx, true);
        } catch (ValidationException e) {
            throw S1SystemError.wrap(e);
        }
    }

    public Map<String,Object> validate(Map<String,Object> data)
            throws ValidationException {
        return validate(data, null);
    }

    public Map<String,Object> validate(Map<String,Object> data, Map<String,Object> ctx)
            throws ValidationException {
        return validate(data, false, false, ctx);
    }

    public Map<String,Object> validate(Map<String,Object> data, boolean expand, boolean deep, Map<String,Object> ctx)
            throws ValidationException {
        return validate(data,expand,deep,ctx,false).getValidatedData();
    }

    protected ValidateResultBean validate(Map<String,Object> data, boolean expand, boolean deep, Map<String,Object> ctx, boolean quite)
            throws ValidationException {
        if(ctx==null)
            ctx = Objects.newHashMap();
        Map<String,Object> validatedData = Objects.copy(data);
        ObjectSchema resolvedSchema = copyAndReset();
        resolvedSchema.resolved = true;
        resolvedSchema.rootMapAttribute.setData(validatedData);

        List<ObjectSchemaAttribute> attrs = Objects.newArrayList();
        for(ObjectSchemaAttribute a:resolvedSchema.attributes){
            Object dt = validatedData.containsKey(a.getName())?validatedData.get(a.getName()):null;
            ObjectSchemaAttribute va = a.validate(dt, expand, deep, ctx, quite);
            attrs.add(va);
            if(va.getData()==null)
                validatedData.remove(va.getName());
            else
                validatedData.put(va.getName(),va.getData());
        }
        resolvedSchema.attributes = attrs;

        return new ValidateResultBean(resolvedSchema,validatedData);
    }

    public ObjectSchema copyAndReset(){
        ObjectSchema a = new ObjectSchema();
        a.mode = mode;
        a.attributes = Objects.newArrayList();
        for(ObjectSchemaAttribute a1:attributes){
            a.attributes.add(a1.copyAndReset());
        }
        a.types = Objects.newArrayList();
        for(ObjectSchemaType a1:types){
            a.types.add(a1.copy());
        }
        a.setAttributesSchema();
        return a;
    }

    /**
     *
     */
    public static class ValidateResultBean {
        private ObjectSchema resolvedSchema;
        private Map<String,Object> validatedData;

        public ValidateResultBean(ObjectSchema resolvedSchema, Map<String, Object> validatedData) {
            this.resolvedSchema = resolvedSchema;
            this.validatedData = validatedData;
        }

        public ObjectSchema getResolvedSchema() {
            return resolvedSchema;
        }

        public Map<String, Object> getValidatedData() {
            return validatedData;
        }
    }
}
