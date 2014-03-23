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

import java.util.HashMap;
import java.util.Map;

/**
 * User bean
 */
public class UserBean extends HashMap<String,Object>{

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String FULL_NAME = "fullName";

    public UserBean(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public UserBean(int initialCapacity) {
        super(initialCapacity);
    }

    public UserBean() {
        super();
    }

    public UserBean(Map<? extends String, ?> m) {
        super(m);
    }

    /**
     *
     * @return
     */
    public String getId() {
        return Objects.get(this,ID);
    }

    /**
     *
     * @return
     */
    public String getName() {
        return Objects.get(this,NAME);
    }

    /**
     *
     * @return
     */
    public String getFullName() {
        return Objects.get(this, FULL_NAME);
    }
}
