package org.s1.user;

import org.s1.objects.Objects;
import org.s1.objects.schema.ComplexType;

import java.util.Map;

/**
 * Complex type for user
 */
public class UserComplexType extends ComplexType{

    @Override
    public Map<String, Object> expand(Map<String, Object> m, boolean expand) throws Exception {
        String id = Objects.get(m,"id");
        m.putAll(Users.getUser(id));
        return m;
    }

    @Override
    public Map<String, Object> validate(Map<String, Object> m) throws Exception {
        String id = Objects.get(m,"id");
        if(Objects.isNullOrEmpty(id))
            throw new Exception("user id is missing");
        return m;
    }
}
