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

package org.s1.lifecycle;

import java.util.Map;

/**
 * Base class for lifecycle action
 */
public abstract class LifecycleAction {

    protected String name = null;
    protected Map<String,Object> config = null;

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
    public Map<String, Object> getConfig() {
        return config;
    }

    /**
     * Initializing with name and config
     *
     * @param name
     * @param config
     */
    public void init(String name, Map<String,Object> config) {
        this.name = name;
        this.config = config;
    }

    /**
     * Business logic stub
     */
    public abstract void start();

    public abstract void stop();

}