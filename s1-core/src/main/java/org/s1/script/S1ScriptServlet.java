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

import org.s1.misc.FileUtils;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.s1.weboperation.MapWebOperation;
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
import java.util.Map;

/**
 * S1 script servlet
 */
public class S1ScriptServlet extends HttpServlet{

    private static final Logger LOG = LoggerFactory.getLogger(S1ScriptServlet.class);

    private S1ScriptEngine scriptEngine;
    private String exprStart;
    private String exprEnd;
    private String codeStart;
    private String codeEnd;

    protected S1ScriptEngine getScriptEngine(){
        if(scriptEngine==null){
            synchronized (this){
                if (scriptEngine ==null){
                    scriptEngine = new S1ScriptEngine("scriptServlet.scriptEngine");
                    exprStart = Options.getStorage().getSystem("scriptServlet.exprStart","{{");
                    exprEnd = Options.getStorage().getSystem("scriptServlet.exprEnd","}}");
                    codeStart = Options.getStorage().getSystem("scriptServlet.codeStart","<%s1");
                    codeEnd = Options.getStorage().getSystem("scriptServlet.codeEnd","%>");
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
        String path = getServletContext().getRealPath("/");
        if(path.startsWith("WEB-INF")){
            resp.setStatus(403);
            return;
        }
        String page = req.getRequestURI().substring(req.getContextPath().length());
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        String res = "";
        try {
            res = get("/", path, page, MapWebOperation.convertRequestToMap(req), Objects.newSOHashMap());
        }catch (Throwable e){
            LOG.error("Page: "+page+" error",e);
            resp.setStatus(500);
        }
        resp.getOutputStream().write(res.getBytes(Charset.forName("UTF-8")));
    }

    protected String get(String currentPath, final String dir, String file, final Map<String,Object> params, Map<String,Object> local) throws Exception{
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


        LOG.debug("Rendering page: " + page);
        String text = FileUtils.readFileToString(new File(page),"UTF-8");
        text = getScriptEngine().template(text, Objects.newSOHashMap(
                "pageParams",params,
                "templateParams",local,
                "include",new ScriptFunction(new Context(10000),Objects.newArrayList("path","params")) {
                    @Override
                    public Object call() throws ScriptException {
                        Map<String,Object> l = getContext().get("params");
                        String p = getContext().get(String.class,"path");
                        try{
                            return get(cp,dir,p,params,l);
                        }catch (Throwable e){
                            LOG.error("Inner page: "+p+" error",e);
                            return "";
                        }
                    }
                }
        ),exprStart,exprEnd,codeStart,codeEnd);
        return text;
    }
}
