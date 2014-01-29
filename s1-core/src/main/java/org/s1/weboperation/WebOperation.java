package org.s1.weboperation;

import org.s1.cluster.Session;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * s1v2
 * User: GPykhov
 * Date: 23.01.14
 * Time: 18:26
 */
public abstract class WebOperation<I,O> {

    private static final Logger LOG = LoggerFactory.getLogger(WebOperation.class);

    /**
     *
     */
    protected Map<String,Object> config;

    public WebOperation(){
    }

    public void setConfig(Map<String, Object> config){
        this.config = config;
    }

    /**
     * Override this method to provide some logic
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
     * @param request
     * @return
     */
    protected abstract I parseInput(HttpServletRequest request) throws Exception;

    /**
     * Format operation output
     * @param out
     * @param error
     * @param request
     * @param response
     */
    protected abstract void formatOutput(O out, boolean error,
                                HttpServletRequest request, HttpServletResponse response) throws Exception;

    /**
     * Transform error
     * @param e
     * @param request
     * @param response
     * @return
     */
    protected abstract O transformError(Throwable e, HttpServletRequest request, HttpServletResponse response);

    /**
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
        }else if (LOG.isDebugEnabled()) {
            if (s.length() > 4000)
                s = s.substring(0, 4000) + "...";
            LOG.debug("Request params: " + s);
        }
    }

    /**
     *
     * @param params
     * @return
     */
    protected String inToString(I params){
        return params.toString();
    }

    /**
     *
     * @param method
     * @param request
     */
    protected void logRequest(String method, HttpServletRequest request) {
        if(LOG.isDebugEnabled()){
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
        }else if (LOG.isDebugEnabled()) {
            if (s.length() > 4000)
                s = s.substring(0, 4000) + "...";
            LOG.debug("Process result: " + s);
        }
    }

    /**
     *
     * @param out
     * @return
     */
    protected String outToString(O out){
        return out.toString();
    }

    /**
     *
     * @param time
     */
    protected void logResult(long time) {
        if(LOG.isDebugEnabled())
            LOG.debug("Request finished in, ms: " + time);
    }

    /**
     *
     * @param e
     */
    protected void logError(Throwable e) {
        LOG.info("Request error: "+e.getMessage(), e);
    }

    /**
     *
     */
    public static final String COOKIE = "S1_ID";

    /**
     *
     * @param req
     * @param resp
     */
    public static void runWithinSession(HttpServletRequest req, HttpServletResponse resp, Closure<String,Object> cl){
        String id = null;
        if(req.getCookies()!=null){
            for(Cookie it: req.getCookies()){
                if(COOKIE.equals(it.getName()))
                    id = it.getValue();
            }
        }
        if(id==null){
            id = UUID.randomUUID().toString();
            resp.addCookie(new Cookie(COOKIE,id));
        }
        try{
            Session.run(id,cl);
        }catch (ClosureException e){
            throw e.toSystemError();
        }
    }

    /**
     * Main method, that makes request processing. Called from DispatcherServlet
     * @param method
     * @param request
     * @param response
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     */
    public void request(final String method, final HttpServletRequest request,
                        final HttpServletResponse response) throws ServletException, IOException {
        runWithinSession(request,response,new Closure<String, Object>() {
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
     *
     * @param method
     * @throws MethodNotFoundException
     */
    public static void throwMethodNotFound(String method) throws MethodNotFoundException{
        throw new MethodNotFoundException("Method "+method+" not found");
    }

}
