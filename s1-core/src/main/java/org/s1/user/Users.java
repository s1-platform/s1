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

import org.s1.S1SystemError;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User helper class
 */
public class Users {

    private static final Logger LOG = LoggerFactory.getLogger(Users.class);

    private static volatile UserFactory userFactory;

    private static synchronized UserFactory getUserFactory(){
        if(userFactory==null){
            String cls = Options.getStorage().getSystem("users.factoryClass",UserFactory.class.getName());
            try{
                userFactory = (UserFactory) Class.forName(cls).newInstance();
            }catch (Throwable e){
                LOG.warn("Cannot initialize UserFactory ("+cls+") :"+e.getClass().getName()+": "+e.getMessage());
                throw S1SystemError.wrap(e);
            }
        }
        return userFactory;
    }

    /**
     *
     * @param id
     * @return
     */
    public static UserBean getUser(String id){
        return getUserFactory().getUser(id);
    }

    /**
     *
     * @param user
     * @param role
     * @return
     */
    public static boolean isUserInRole(UserBean user, String role){
        return getUserFactory().isUserInRole(user,role);
    }

}
