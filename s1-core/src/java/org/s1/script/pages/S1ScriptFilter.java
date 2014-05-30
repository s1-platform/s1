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

package org.s1.script.pages;

import org.s1.S1SystemError;
import org.s1.cache.Cache;
import org.s1.cluster.Session;
import org.s1.misc.Closure;
import org.s1.misc.FileUtils;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.s1.script.Context;
import org.s1.script.S1ScriptEngine;
import org.s1.script.errors.ScriptException;
import org.s1.script.function.ScriptFunction;
import org.s1.script.function.URLFunctionSet;
import org.s1.user.UserBean;
import org.s1.user.Users;
import org.s1.weboperation.MapWebOperation;
import org.s1.weboperation.WebOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;

/**
 * Script pages filter
 */
public class S1ScriptFilter implements Filter{

    private static final Logger LOG = LoggerFactory.getLogger(S1ScriptFilter.class);

    private S1ScriptEngine scriptEngine;
    private boolean debug;
    private Cache pageCache;

    protected String getPage(String path){
        //if(path.endsWith(".html"))
        //    return path;
        if(path.equals("/")){
            return "/index.html";
        }else{
            return path+".html";
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        scriptEngine = new S1ScriptEngine("pages.scriptEngine");
        debug = Options.getStorage().getSystem(Boolean.class,"pages.debug",false);

        pageCache = new Cache(Options.getStorage().getSystem(Integer.class,"pages.cacheSize",1000));
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)resp;
        String page = request.getRequestURI().substring(request.getContextPath().length());
        if(page.matches(".*/.+\\.\\w+$") || page.matches("^/dispatcher/.*$")){
            chain.doFilter(req, resp);
            return;
        }
        process(request,response);
    }

    @Override
    public void destroy() {

    }

    protected void process(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = null;
        try {
            id = WebOperation.getSessionId(req, resp);
            Session.start(id);

            String path = req.getServletContext().getRealPath("/");
            if (path.startsWith("WEB-INF")) {
                resp.setStatus(403);
                return;
            }
            String page = req.getRequestURI().substring(req.getContextPath().length());
            resp.setContentType("text/html");
            resp.setCharacterEncoding("UTF-8");
            String res = "";
            try {
                res = get("", path, page, MapWebOperation.convertRequestToMap(req), Objects.newSOHashMap(), req, resp);
            } catch (Throwable e) {
                LOG.error("Page: " + page + " error", e);
                resp.setStatus(500);
                if(debug)
                    res = "<b>Page: "+page+" error: "+e.getClass().getName()+": "+e.getMessage()+"</b>";
            }
            resp.getOutputStream().write(res.getBytes(Charset.forName("UTF-8")));
        }finally {
            Session.end(id);
        }
    }

    protected String get(String currentPath, final String dir, String file, final Map<String,Object> params, Map<String,Object> local,
                         final HttpServletRequest req, final HttpServletResponse resp) throws Exception{
        file = getPage(file);
        String page = "";
        if(file.startsWith("/")){
            //abs
            page=dir+"/"+file;
        }else{
            //rel
            page=currentPath+"/"+file;
        }
        page = page.replaceAll("\\\\","/").replaceAll("/+","/").replace("/",File.separator);

        final String cp = page.substring(0,page.lastIndexOf(File.separator));

        final String url = req.getRequestURL().toString();
        final String context = req.getContextPath();
        final UserBean user = Users.getUser(Session.getSessionBean().getUserId());
        final Map<String,Object> headers = Objects.newHashMap();
        final Map<String,Object> responseHeaders = Objects.newHashMap();

        //final Map<String,String> includePages = Objects.newHashMap();
        //final Map<String,Map<String,Object>> includeParams = Objects.newHashMap();

        Enumeration<String> eh = req.getHeaderNames();
        while(eh.hasMoreElements()){
            String s = eh.nextElement();
            headers.put(s,req.getHeader(s));
        }
        final Map<String,Object> layout = Objects.newHashMap();

        LOG.debug("Rendering page: " + page);
        String text = null;
        if(debug) {
            scriptEngine.invalidateCache(page);
            text = FileUtils.readFileToString(new File(page), "UTF-8");
        } else {
            final String _page = page;
            text = pageCache.get(page, new Closure<String, String>() {
                @Override
                public String call(String input) {
                    try {
                        scriptEngine.invalidateCache(_page);
                        String s = FileUtils.readFileToString(new File(_page), "UTF-8");
                        if (s == null)
                            s = "";
                        return s;
                    } catch (IOException e) {
                        throw S1SystemError.wrap(e);
                    }
                }
            });
        }

        if(Objects.isNullOrEmpty(text)){
            resp.setStatus(404);
            LOG.info("Page not found: "+page);
            return "";
        }
        String port = "";
        if(req.getScheme().equals("http") && req.getServerPort()!=80)
            port = ":"+req.getServerPort();
        else if(req.getScheme().equals("https") && req.getServerPort()!=443)
            port = ":"+req.getServerPort();
        String query = req.getQueryString();
        if(!Objects.isNullOrEmpty(query)) {
            URLFunctionSet url_fs = new URLFunctionSet();
            query = url_fs.removeParams("url?" + query, Objects.newArrayList("_pjax")).split("\\?", -1)[1];
        }
        text = scriptEngine.template(page, text, Objects.newSOHashMap(
                "page",Objects.newSOHashMap(
                        "params",params,
                        "headers",headers,
                        "responseHeaders",responseHeaders,
                        "url",url,
                        "method",req.getMethod().toLowerCase(),
                        "query",query,
                        "uri",req.getRequestURI(),
                        "scheme",req.getScheme(),
                        "host",req.getServerName(),
                        "hostname",req.getServerName()+port,
                        "port",req.getServerPort(),
                        "relative",req.getRequestURI().substring(req.getContextPath().length()),
                        "context",context,
                        "debug",debug,
                        "isAnonymous",Session.getSessionBean().getUserId().equals(Session.ANONYMOUS),
                        "user",user
                ),
                "args",local,
                "include",new ScriptFunction(new Context(10000),Objects.newArrayList("path","params")) {
                    @Override
                    public Object call(Context ctx) throws ScriptException {
                        Map<String,Object> l = ctx.get("params");
                        String p = ctx.get(String.class,"path");
                        String id = UUID.randomUUID().toString();
                        //includePages.put(id,p);
                        //includeParams.put(id,l);
                        //return "---"+id+"---";
                        String t = "";
                        try {
                            t = get(cp, dir, p, params, l, req, resp);
                        } catch (Throwable e) {
                            LOG.error("Inner page: " + p + " error", e);
                            if (debug)
                                t = "<b>Inner page: " + p + " error: "
                                        + e.getClass().getName() + ": " + e.getMessage() + "</b>";
                            t = "";
                        }
                        return t;
                    }
                },
                "layout",new ScriptFunction(new Context(10000),Objects.newArrayList("path","params")) {
                    @Override
                    public Object call(Context ctx) throws ScriptException {
                        Map<String,Object> l = ctx.get("params");
                        String p = ctx.get(String.class,"path");
                        layout.put("page",p);
                        layout.put("params",l);
                        return "";
                    }
                }
        ));

        for(String s:responseHeaders.keySet()){
            resp.setHeader(s,Objects.get(String.class,responseHeaders,s));
        }

        /*for(String id:includePages.keySet()) {
            String p = includePages.get(id);
            Map<String,Object> l = includeParams.get(id);
            String t = "";
            try {
                t = get(cp, dir, p, params, l, req, resp);
            } catch (Throwable e) {
                LOG.error("Inner page: " + p + " error", e);
                if (debug)
                    t = "<b>Inner page: " + p + " error: "
                            + e.getClass().getName() + ": " + e.getMessage() + "</b>";
                t = "";
            }
            text = text.replace("---"+id+"---",t);
        }*/

        if(!layout.isEmpty()){
            String p = Objects.get(layout,"page");
            Map<String,Object> l = Objects.get(layout,"params");
            l.put("layout_content",text);
            try{
                text = get(cp,dir,p,params,l,req,resp);
            }catch (Throwable e){
                if(debug)
                    text = "<b>Layout page: "+p+" error: "+e.getClass().getName()+": "+e.getMessage()+"</b>";
                LOG.error("Layout page: "+p+" error: "+e.getMessage(),e);
            }
        }

        return text;
    }
}
