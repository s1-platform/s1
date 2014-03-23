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

import org.s1.cluster.Session;
import org.s1.objects.Objects;
import org.s1.weboperation.MapWebOperation;
import org.s1.weboperation.WebOperationMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * User operation
 */
public class UserOperation extends MapWebOperation {

    @WebOperationMethod
    public Map<String, Object> logout(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String,Object> res = Objects.newHashMap();
        Session.getSessionBean().setUserId(null);
        return res;
    }

    @WebOperationMethod
    public Map<String, Object> whoAmI(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return Users.getUser(Session.getSessionBean().getUserId());
    }

    @WebOperationMethod
    public Map<String, Object> getUser(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String id = Objects.get(params,"id");
        return Users.getUser(id);
    }

    @Override
    protected Map<String, Object> process(String method, Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return processClassMethods(this,method,params,request,response);
    }

}
