package org.s1.objects.schema;

import org.s1.objects.Objects;

import java.util.Map;

/**
 * Simple type attribute
 */
public class SimpleTypeAttribute extends ObjectSchemaAttribute<Object> {

    SimpleTypeAttribute(){
    }

    /**
     *
     * @param name
     * @param label
     * @param type
     */
    public SimpleTypeAttribute(String name, String label, Class type) {
        super(name,label,type.getSimpleName());
        this.type = Objects.resolveType(this.type).getSimpleName();
    }

    @Override
    protected void validateType(boolean expand, boolean deep,  Map<String,Object> ctx, boolean quite) throws Exception{
        if(data!=null)
            data = Objects.cast(data, type);
    }
}
