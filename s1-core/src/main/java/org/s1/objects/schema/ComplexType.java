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
import org.s1.objects.schema.errors.ValidationException;

import java.util.Map;

/**
 * Base type for complex types
 */
public abstract class ComplexType {

    protected Map<String,Object> config = Objects.newHashMap();

    /**
     *
     * @return
     */
    public Map<String, Object> getConfig() {
        return config;
    }

    /**
     *
     * @param config
     */
    public void setConfig(Map<String, Object> config) {
        if(config ==null)
            config = Objects.newHashMap();
        this.config = config;
    }

    /**
     *
     * @param m
     * @return
     * @throws Exception
     */
    public abstract Map<String, Object> expand(Map<String, Object> m, boolean expand) throws Exception;

    /**
     *
     * @param m
     * @return
     * @throws Exception
     */
    public abstract Map<String, Object> validate(Map<String, Object> m) throws ValidationException;

}
