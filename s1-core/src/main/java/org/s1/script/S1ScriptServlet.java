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

package org.s1.script;

import org.s1.cluster.Session;
import org.s1.misc.FileUtils;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.s1.user.UserBean;
import org.s1.user.Users;
import org.s1.weboperation.MapWebOperation;
import org.s1.weboperation.WebOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Map;

/**
 * S1 script servlet
 */
public class S1ScriptServlet extends HttpServlet{

    private static final Logger LOG = LoggerFactory.getLogger(S1ScriptServlet.class);

    private S1ScriptEngine scriptEngine;

    protected S1ScriptEngine getScriptEngine(){
        if(scriptEngine==null){
            synchronized (this){
                if (scriptEngine ==null){
                    scriptEngine = new S1ScriptEngine("scriptServlet.scriptEngine");
                }
            }
        }
        return scriptEngine;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    protected void process(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = null;
        try {
            id = WebOperation.getSessionId(req, resp);
            Session.start(id);

            String path = getServletContext().getRealPath("/");
            if (path.startsWith("WEB-INF")) {
                resp.setStatus(403);
                return;
            }
            String page = req.getRequestURI().substring(req.getContextPath().length());
            resp.setContentType("text/html");
            resp.setCharacterEncoding("UTF-8");
            String res = "";
            try {
                res = get("/", path, page, MapWebOperation.convertRequestToMap(req), Objects.newSOHashMap(), req, resp);
            } catch (Throwable e) {
                LOG.error("Page: " + page + " error", e);
                resp.setStatus(500);
            }
            resp.getOutputStream().write(res.getBytes(Charset.forName("UTF-8")));
        }finally {
            Session.end(id);
        }
    }

    protected String get(String currentPath, final String dir, String file, final Map<String,Object> params, Map<String,Object> local,
                         final HttpServletRequest req, final HttpServletResponse resp) throws Exception{
        String page = "";
        if(file.startsWith("/")){
            //abs
            page=dir+"/"+file;
        }else{
            //rel
            page=currentPath+"/"+file;
        }
        page = page.replaceAll("\\\\","/").replaceAll("/+","/");
        if(page.endsWith("/"))
            page+="index.html";

        final String cp = page.substring(0,page.lastIndexOf("/"));

        final String url = req.getRequestURL().toString();
        final String context = req.getContextPath();
        final UserBean user = Users.getUser(Session.getSessionBean().getId());
        final Map<String,Object> headers = Objects.newHashMap();
        final Map<String,Object> responseHeaders = Objects.newHashMap();
        Enumeration<String> eh = req.getHeaderNames();
        while(eh.hasMoreElements()){
            String s = eh.nextElement();
            headers.put(s,req.getHeader(s));
        }
        final Map<String,Object> layout = Objects.newHashMap();

        LOG.debug("Rendering page: " + page);
        String text = FileUtils.readFileToString(new File(page),"UTF-8");
        text = getScriptEngine().template(text, Objects.newSOHashMap(
                "page",Objects.newSOHashMap(
                        "params",params,
                        "headers",headers,
                        "responseHeaders",responseHeaders,
                        "url",url,
                        "dir",dir,
                        "context",context,
                        "user",user
                ),
                "args",local,
                "include",new ScriptFunction(new Context(10000),Objects.newArrayList("path","params")) {
                    @Override
                    public Object call() throws ScriptException {
                        Map<String,Object> l = getContext().get("params");
                        String p = getContext().get(String.class,"path");
                        try{
                            return get(cp,dir,p,params,l,req,resp);
                        }catch (Throwable e){
                            LOG.error("Inner page: "+p+" error",e);
                            return "";
                        }
                    }
                },
                "layout",new ScriptFunction(new Context(10000),Objects.newArrayList("path","params")) {
                    @Override
                    public Object call() throws ScriptException {
                        Map<String,Object> l = getContext().get("params");
                        String p = getContext().get(String.class,"path");
                        layout.put("page",p);
                        layout.put("params",l);
                        return "";
                    }
                }
        ));

        for(String s:responseHeaders.keySet()){
            resp.setHeader(s,Objects.get(String.class,responseHeaders,s));
        }

        if(!layout.isEmpty()){
            String p = Objects.get(layout,"page");
            Map<String,Object> l = Objects.get(layout,"params");
            l.put("layout_content",text);
            try{
                text = get(cp,dir,p,params,l,req,resp);
            }catch (Throwable e){
                LOG.error("Layout page: "+p+" error",e);
            }
        }

        return text;
    }
}
