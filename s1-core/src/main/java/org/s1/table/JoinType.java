package org.s1.table;

import org.s1.objects.Objects;
import org.s1.objects.schema.ComplexType;

import java.util.Map;

/**
 * Join complex type
 */
public class JoinType extends ComplexType{

    protected Table getTable(){
        String t = Objects.get(getConfig(),"table");
        return TablesFactory.getTable(t);
    }

    @Override
    public Map<String, Object> expand(Map<String, Object> m, boolean expand) throws Exception {
        String id = Objects.get(m,"id");
        m.putAll(getTable().get(id, Objects.newHashMap(String.class, Object.class,
                Table.CTX_VALIDATE_KEY, true,
                Table.CTX_EXPAND_KEY, true,
                Table.CTX_DEEP_KEY, expand
        )));
        return m;
    }

    @Override
    public Map<String, Object> validate(Map<String, Object> m) throws Exception {
        String id = Objects.get(m,"id");
        getTable().get(id);
        return m;
    }
}
