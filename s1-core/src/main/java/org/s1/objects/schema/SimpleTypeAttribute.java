package org.s1.objects.schema;

import org.s1.objects.Objects;

import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 11.01.14
 * Time: 17:17
 */
public class SimpleTypeAttribute extends ObjectSchemaAttribute<Object> {

    SimpleTypeAttribute(){
    }

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
