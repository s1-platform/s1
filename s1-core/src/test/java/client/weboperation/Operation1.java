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

package client.weboperation;

import org.s1.objects.Objects;
import org.s1.weboperation.MapWebOperation;
import org.s1.weboperation.WebOperationMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 10:43
 */
public class Operation1 extends MapWebOperation {

    @WebOperationMethod
    public Map<String, Object> method1(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        return Objects.newHashMap("a", "1");
    }

    @WebOperationMethod
    public Map<String, Object> method2(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        return Objects.newHashMap("a", "1");
    }

    @Override
    protected Map<String, Object> process(String method, Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        return processClassMethods(this,method,params,request,response);
    }
}
