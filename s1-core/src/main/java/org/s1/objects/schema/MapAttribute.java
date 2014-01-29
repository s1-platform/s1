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
public class MapAttribute extends ObjectSchemaAttribute<Map<String, Object>> {

    protected List<ObjectSchemaAttribute> attributes;

    MapAttribute(){
    }

    public MapAttribute(String name, String label, ObjectSchemaAttribute... args) {
        super(name,label,"Map");
        this.attributes = Arrays.asList(args);
        updateChildSchemaAndParent();
    }

    public MapAttribute(String name, String label, List<ObjectSchemaAttribute> args) {
        super(name, label, "Map");
        if(args!=null)
            this.attributes = args;
        updateChildSchemaAndParent();
    }

    @Override
    public ObjectSchemaAttribute copyAndReset() {
        MapAttribute a = (MapAttribute)super.copyAndReset();
        a.attributes = Objects.newArrayList();
        for(ObjectSchemaAttribute a1:attributes){
            a.attributes.add(a1.copyAndReset());
        }
        return a;
    }

    @Override
    protected void validateType(boolean expand, boolean deep, Map<String, Object> ctx, boolean quite) throws Exception {
        if (data != null && !(data instanceof Map)) {
            throw new Exception("Data is not map");
        }
        if (data != null) {
            List<ObjectSchemaAttribute> attrs = Objects.newArrayList();
            for (ObjectSchemaAttribute a : attributes) {
                Object dt = data.containsKey(a.getName()) ? data.get(a.getName()) : null;
                ObjectSchemaAttribute va = a.validate(dt, expand, deep, ctx, quite);
                if (va.getData() == null)
                    data.remove(a.getName());
                else
                    data.put(a.getName(), va.getData());
                attrs.add(va);
            }
            attributes = attrs;
        }
    }

    protected void updateChildSchemaAndParent() {
        for (ObjectSchemaAttribute a : attributes) {
            a.setParent(this);
            a.setSchema(this.schema);
            a.updateChildSchemaAndParent();
        }
    }

    void fromMap(Map<String, Object> m) throws ObjectSchemaFormatException {
        super.fromMap(m);

        List<Map<String, Object>> attrs = Objects.get(m, "attributes");
        attrs = Objects.copy(attrs);
        this.attributes = Objects.newArrayList();
        if (!Objects.isNullOrEmpty(attrs)) {
            for (Map<String, Object> a : attrs) {
                attributes.add(ObjectSchemaAttribute.createFromMap(a));
            }
        }
        updateChildSchemaAndParent();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = super.toMap();
        List<Map<String, Object>> attrs = Objects.newArrayList();
        for (ObjectSchemaAttribute a : attributes) {
            attrs.add(a.toMap());
        }
        m.put("attributes", attrs);
        return Objects.copy(m);
    }

    public List<ObjectSchemaAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<ObjectSchemaAttribute> attributes) {
        if(attributes!=null)
            this.attributes = Objects.newArrayList();
        else
            this.attributes = Objects.newArrayList();
    }
}
