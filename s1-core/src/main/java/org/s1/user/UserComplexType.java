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

package org.s1.user;

import org.s1.objects.Objects;
import org.s1.objects.schema.ComplexType;
import org.s1.objects.schema.errors.CustomValidationException;
import org.s1.objects.schema.errors.ValidationException;

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
    public Map<String, Object> validate(Map<String, Object> m) throws ValidationException {
        String id = Objects.get(m,"id");
        if(Objects.isNullOrEmpty(id))
            throw new CustomValidationException("User #"+id+" not found");
        return m;
    }
}
