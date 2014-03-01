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

import org.s1.cluster.Session;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;
import org.s1.script.S1ScriptEngine;
import org.s1.user.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for some web actions
 */
public abstract class WebOperation<I, O> {

    private static final Logger LOG = LoggerFactory.getLogger(WebOperation.class);

    /**
     *
     */
    protected Map<String, Object> config;

    /**
     *
     */
    public WebOperation() {
    }

    /**
     * Configuration
     *
     * @param config
     */
    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    /**
     * Override this method to provide some logic
     *
     * @param method
     * @param params
     * @param request
     * @param response
     * @return
     */
    protected abstract O process(String method, I params,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception;


    /**
     * Parse operation input
     *
     * @param request
     * @return
     */
    protected abstract I parseInput(HttpServletRequest request) throws Exception;

    /**
     * Format operation output
     *
     * @param out
     * @param error
     * @param request
     * @param response
     */
    protected abstract void formatOutput(O out, boolean error,
                                         HttpServletRequest request, HttpServletResponse response) throws Exception;

    /**
     * Transform error
     *
     * @param e
     * @param request
     * @param response
     * @return
     */
    protected abstract O transformError(Throwable e, HttpServletRequest request, HttpServletResponse response);

    /**
     * Log input params
     *
     * @param params
     */
    protected void logInParams(I params) {
        String s = "";
        if (params != null) {
            s = inToString(params);
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("Request params: " + s);
        } else if (LOG.isDebugEnabled()) {
            if (s.length() > 4000)
                s = s.substring(0, 4000) + "...";
            LOG.debug("Request params: " + s);
        }
    }

    /**
     * Convert input params to string
     *
     * @param params
     * @return
     */
    protected String inToString(I params) {
        return params.toString();
    }

    /**
     * Log request
     *
     * @param method
     * @param request
     */
    protected void logRequest(String method, HttpServletRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Request started\n" +
                    "   Class: " + this.getClass().getName() + "\n" +
                    "   Method: " + method + "\n" +
                    "   User id: " + Session.getSessionBean().getUserId() + "\n" +
                    "   Session id: " + Session.getSessionBean().getId() + "\n" +
                    "   Remote address: " + request.getRemoteAddr() + "\n" +
                    "   User-Agent: " + request.getHeader("User-Agent"));
        }
    }

    /**
     * Log output data
     *
     * @param out
     */
    protected void logOut(O out) {
        String s = "";
        if (out != null) {
            s = outToString(out);
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("Process result: " + s);
        } else if (LOG.isDebugEnabled()) {
            if (s.length() > 4000)
                s = s.substring(0, 4000) + "...";
            LOG.debug("Process result: " + s);
        }
    }

    /**
     * Convert output data to string
     *
     * @param out
     * @return
     */
    protected String outToString(O out) {
        return out.toString();
    }

    /**
     * Log result
     *
     * @param time
     */
    protected void logResult(long time) {
        if (LOG.isDebugEnabled())
            LOG.debug("Request finished in, ms: " + time);
    }

    /**
     * Log error
     *
     * @param e
     */
    protected void logError(Throwable e) {
        LOG.info("Request error: " + e.getMessage(), e);
    }

    /**
     * Session id cookie
     */
    public static final String COOKIE = "S1_ID";

    /**
     * Run closure within session
     *
     * @param req
     * @param resp
     * @param cl
     */
    public static void runWithinSession(HttpServletRequest req, HttpServletResponse resp, Closure<String, Object> cl) {
        String id = null;
        if (req.getCookies() != null) {
            for (Cookie it : req.getCookies()) {
                if (COOKIE.equals(it.getName()))
                    id = it.getValue();
            }
        }
        if (id == null) {
            id = UUID.randomUUID().toString();
            resp.addCookie(new Cookie(COOKIE, id));
        }
        try {
            Session.run(id, cl);
        } catch (ClosureException e) {
            throw e.toSystemError();
        }
    }

    /**
     * Main method, that makes request processing. Called from DispatcherServlet
     *
     * @param method
     * @param request
     * @param response
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     */
    public void request(final String method, final HttpServletRequest request,
                        final HttpServletResponse response) throws ServletException, IOException {
        runWithinSession(request, response, new Closure<String, Object>() {
            @Override
            public Object call(String input) throws ClosureException {

                long t = System.currentTimeMillis();
                try {
                    String requestId = UUID.randomUUID().toString();

                    // set MDC
                    MDC.put("requestId", requestId);

                    logRequest(method, request);

                    I params = parseInput(request);
                    logInParams(params);

                    //check access
                    checkAccess(method, params, request);

                    O out = process(method, params, request, response);

                    if (out != null) {
                        logOut(out);
                        formatOutput(out, false, request, response);
                    }
                } catch (Throwable e) {
                    logError(e);
                    try {
                        O out = transformError(e, request, response);
                        formatOutput(out, true, request, response);
                    } catch (Exception ex) {
                        LOG.error("Error preparing exception output", ex);
                    }
                }
                logResult(System.currentTimeMillis() - t);
                return null;
            }
        });
    }

    /**
     * Throws method not found
     *
     * @param method
     * @throws MethodNotFoundException
     */
    public static void throwMethodNotFound(String method) throws MethodNotFoundException {
        throw new MethodNotFoundException("Method " + method + " not found");
    }

    /**
     *
     * @param method
     * @param params
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    public static <I,O> O processClassMethods(WebOperation<I,O> T, String method, I params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        Method mt = null;
        for(Method m:T.getClass().getDeclaredMethods()){
            if(m.getName().equals(method) && m.getAnnotation(WebOperationMethod.class)!=null){
                mt = m;
                break;
            }
        }
        if(mt!=null){
            try{
                return (O)mt.invoke(T,params,request,response);
            }catch (InvocationTargetException e){
                if(e.getCause()!=null)
                    throw (Exception)e.getCause();
                throw e;
            }
        } else{
            throwMethodNotFound(method);
        }
        return null;
    }

    /**
     *
     * @param request
     * @return
     */
    public static String getClientIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     *
     * @param method
     * @param params
     * @param request
     * @throws AccessDeniedException
     */
    protected void checkAccess(String method, I params, HttpServletRequest request) throws AccessDeniedException {
        String userId = Session.getSessionBean().getUserId();
        boolean ok = true;

        String ip = getClientIpAddr(request);
        Map<String,String> headers = Objects.newHashMap();
        Enumeration<String> en = request.getHeaderNames();
        while(en.hasMoreElements()){
            String n = en.nextElement();
            headers.put(n,request.getHeader(n));
        }
        //ip white list
        if(ok){
            if(Objects.get(config, "ipWhiteList")!=null){
                List<String> list = Objects.get(config, "ipWhiteList");
                ok = list.contains(ip);
            }
        }

        //ip black list
        if(ok){
            if(Objects.get(config, "ipBlackList")!=null){
                List<String> list = Objects.get(config, "ipBlackList");
                ok = !list.contains(ip);
            }
        }

        //access script
        if(ok){
            String s = Objects.get(config, "access");
            if(!Objects.isNullOrEmpty(s)){
                try{
                    ok = new S1ScriptEngine().evalInFunction(Boolean.class,s,
                            Objects.newHashMap(String.class, Object.class,
                                    "userId", userId,
                                    "ip", ip,
                                    "headers",headers,
                                    "method",method,
                                    "params",params));
                }catch (Throwable e){
                    if(LOG.isDebugEnabled())
                        LOG.debug("Access script error: "+e.getMessage(),e);
                }
            }
        }

        if(!ok)
            throw new AccessDeniedException("Access is denied");
    }

}
