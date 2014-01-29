package org.s1.objects.schema;

import org.s1.misc.Closure;
import org.s1.objects.Objects;

import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 11.01.14
 * Time: 17:17
 */
public class ReferenceAttribute extends ObjectSchemaAttribute<Map<String,Object>> {

    ReferenceAttribute(){
    }

    public ReferenceAttribute(String name, String label, String type) {
        super(name,label,type);
    }

    void fromMap(Map<String,Object> m) throws ObjectSchemaFormatException{
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

    MapAttribute resolve() throws Exception{
        ObjectSchemaType t = Objects.find(schema.getTypes(), new Closure<ObjectSchemaType, Boolean>() {
            @Override
            public Boolean call(ObjectSchemaType input) {
                return input.getName().equals(type);
            }
        });
        if(t==null)
            throw new Exception("reference not found");

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
