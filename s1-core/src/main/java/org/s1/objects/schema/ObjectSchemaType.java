package org.s1.objects.schema;

import org.s1.objects.Objects;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 11.01.14
 * Time: 17:17
 */
public class ObjectSchemaType {

    private String name;

    private List<ObjectSchemaAttribute> attributes = Objects.newArrayList();

    ObjectSchemaType(Map<String, Object> m) throws ObjectSchemaFormatException {
        fromMap(m);
    }

    public ObjectSchemaType(String name, List<ObjectSchemaAttribute> attributes) {
        this.name = name;
        if(attributes!=null)
            this.attributes = attributes;
    }

    public ObjectSchemaType(String name, ObjectSchemaAttribute... args) {
        this.name = name;
        this.attributes = Arrays.asList(args);
    }

    public String getName() {
        return name;
    }

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

    public Map<String,Object> toMap(){
        List<Map<String,Object>> attrs = Objects.newArrayList();
        for(ObjectSchemaAttribute a:attributes){
            attrs.add(a.toMap());
        }
        Map<String,Object> m = Objects.newHashMap("name", name, "attributes", attrs);
        return m;
    }

    public ObjectSchemaType copy(){
        ObjectSchemaType a = new ObjectSchemaType(name);
        a.attributes = Objects.newArrayList();
        for(ObjectSchemaAttribute a1:attributes){
            a.attributes.add(a1.copyAndReset());
        }
        return a;
    }

}
