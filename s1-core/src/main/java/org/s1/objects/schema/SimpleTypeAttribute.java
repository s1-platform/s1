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
