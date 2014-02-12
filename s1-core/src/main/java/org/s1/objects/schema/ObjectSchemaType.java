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
