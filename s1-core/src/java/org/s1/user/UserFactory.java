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

/**
 * Abstract user factory
 */
public class UserFactory {

    /**
     *
     * @param id
     * @return
     */
    public UserBean getUser(String id) {
        return new UserBean(Objects.newHashMap(String.class,Object.class,"id",id));
    }

    /**
     *
     * @param user
     * @param role
     * @return
     */
    public boolean isUserInRole(UserBean user, String role){
        return false;
    }

}
