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

package org.s1.weboperation;

import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispatcher servlet. Dispatches requests between web operations. <br>
 *
 * <pre>
 * &lt;!-- Main dispatcher servlet. Serves business logic of application. -->
 * &lt;servlet>
 *  &lt;servlet-name>dispatcher&lt;/servlet-name>
 *  &lt;servlet-class>org.s1.weboperation.DispatcherServlet&lt;/servlet-class>
 *  &lt;load-on-startup>1&lt;/load-on-startup>
 * &lt;/servlet>
 * &lt;servlet-mapping>
 *  &lt;servlet-name>dispatcher&lt;/servlet-name>
 *  &lt;url-pattern>/dispatcher/*&lt;/url-pattern>
 * &lt;/servlet-mapping>
 * </pre>
 */
@MultipartConfig
public class DispatcherServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(DispatcherServlet.class);

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        process(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
        process(request, response);
    }

    /**
     * Get method from request.
     *
     * @param request
     * @return
     */
    protected String getMethod(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String q = uri.substring((request.getContextPath()+request.getServletPath()).length()+1);
        int i = q.indexOf("/");
        int j = q.indexOf(".");
        String method = null;
        if(Math.max(i,j)>0){
            int k = Math.min(i,j);
            if(k<0)
                k = Math.max(i,j);
            method = q.substring(k+1);
        }
        return method;
    }

    private static Map<String,WebOperation> cache = new ConcurrentHashMap<String, WebOperation>();

    /**
     * Get WebOperation by name
     *
     * @param name
     * @return
     * @throws WebOperationNotFoundException
     */
    public static WebOperation getOperationByName(final String name) throws WebOperationNotFoundException{
        if(!cache.containsKey(name)){
            WebOperation wo = null;

            Map<String,Object> cls = Objects.find((List<Map<String,Object>>)Options.getStorage().getSystem(List.class,"webOperations", new ArrayList()),new Closure<Map<String, Object>, Boolean>() {
                @Override
                public Boolean call(Map<String, Object> input) {
                    return name.equals(Objects.get(input, "name"));
                }
            });
            if(!Objects.isNullOrEmpty(cls)){
                try{
                    wo = (WebOperation)Class.forName(Objects.get(String.class,cls,"class")).newInstance();
                    wo.setConfig(Objects.get(cls, "config", Objects.newHashMap(String.class, Object.class)));
                }catch (Exception e){
                    throw new WebOperationNotFoundException("Cannot initialize WebOperation ("+cls+"): "+e.getMessage(),e);
                }
            }
            if(wo == null)
                throw new WebOperationNotFoundException("WebOperation "+name+" not found");
            cache.put(name,wo);
        }
        return cache.get(name);
    }

    /**
     * Get operation from request
     *
     * @param request
     * @return
     * @throws WebOperationNotFoundException
     */
    protected WebOperation getOperation(HttpServletRequest request) throws WebOperationNotFoundException{
        String uri = request.getRequestURI();
        String q = uri.substring((request.getContextPath()+request.getServletPath()).length()+1);
        int i = q.indexOf("/");
        int j = q.indexOf(".");
        String name = q;
        if(Math.max(i,j)>0){
            int k = Math.min(i,j);
            if(k<0)
                k = Math.max(i,j);
            name = q.substring(0,k);
        }
        return getOperationByName(name);
    }

    /**
     * Process GET|POST request: get {@link org.s1.weboperation.WebOperation},
     * and call {@link org.s1.weboperation.WebOperation#request(String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     *
     * @param request
     * @param response
     * @throws javax.servlet.ServletException
     * @throws IOException
     */
    protected void process(HttpServletRequest request,
                           HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        WebOperation operation = null;
        try{
            operation = getOperation(request);
        }catch (WebOperationNotFoundException e){
            LOG.warn("WebOperation found problems: "+e.getMessage(),e);
            response.setStatus(404);
            return;
        }

        String method = getMethod(request);
        operation.request(method, request, response);
    }

}
