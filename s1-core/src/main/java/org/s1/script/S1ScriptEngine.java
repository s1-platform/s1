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

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;
import org.s1.S1SystemError;
import org.s1.cache.Cache;
import org.s1.cluster.Session;
import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.s1.options.OptionsStorage;
import org.s1.script.errors.ScriptException;
import org.s1.script.errors.ScriptLimitException;
import org.s1.script.errors.SyntaxException;
import org.s1.script.function.ScriptFunction;
import org.s1.script.function.ScriptFunctionSet;
import org.s1.script.function.SystemFunctionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S1 Script engine
 */
public class S1ScriptEngine {

    private static final Logger LOG = LoggerFactory.getLogger(S1ScriptEngine.class);

    public static final String OPTIONS_KEY = "scriptEngine";
    private static final ExecutorService service;

    static{
        int ps = Options.getStorage().getSystem(Integer.class,OPTIONS_KEY+".threadCount",500);
        service = Executors.newFixedThreadPool(ps,new ThreadFactory() {
            private AtomicInteger i = new AtomicInteger(-1);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "S1ScriptEngineThread-"+i.incrementAndGet());
            }
        });
    }

    private List<Map<String,Object>> functions = Objects.newArrayList();
    private long timeLimit = 0;
    private long sizeLimit = 0;
    private long memoryLimit = 0;

    private Cache astCache;
    private Cache templateCache;

    /**
     * Create with path='scriptEngine'
     */
    public S1ScriptEngine(){
        this(OPTIONS_KEY);
    }

    /**
     * Create
     *
     * @param path system options path
     */
    public S1ScriptEngine(String path){
        this(OptionsStorage.SYSTEM_PROPERTIES,path);
    }

    /**
     * Create
     *
     * @param options name of options
     * @param path options path
     */
    public S1ScriptEngine(String options, String path){
        functions = Options.getStorage().get(options,path+".functions",functions);
        timeLimit = Options.getStorage().get(options,path+".timeLimit",30000L);
        memoryLimit = Options.getStorage().get(options,path+".memoryLimit",16*1024*1024L);
        sizeLimit = Options.getStorage().get(options,path+".sizeLimit",16*1024*1024L);
        astCache = new Cache(Options.getStorage().get(Integer.class,options,path+".astCacheSize",1000));
        templateCache = new Cache(Options.getStorage().get(Integer.class,options,path+".templateCacheSize",1000));
    }

    /**
     * Functions classes
     * @return
     */
    public List<Map<String,Object>> getFunctions() {
        return functions;
    }

    public void setFunctions(List<Map<String,Object>> functions) {
        this.functions = functions;
    }

    /**
     * Time limit in ms
     *
     * @return
     */
    public long getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(long timeLimit) {
        this.timeLimit = timeLimit;
    }

    /**
     * Size limit in characters
     *
     * @return
     */
    public long getSizeLimit() {
        return sizeLimit;
    }

    public void setSizeLimit(long sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    /**
     * Memory limit in bytes
     *
     * @return
     */
    public long getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(long memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    /**
     *
     * @param cls
     * @param script
     * @param data
     * @param <T>
     * @return
     */
    public <T> T evalInFunction(Class<T> cls, String script, Map<String,Object> data){
        return evalInFunction(null,cls,script,data);
    }

    public <T> T evalInFunction(String name, Class<T> cls, String script, Map<String,Object> data){
        return Objects.cast(eval(name, "(function(){" + script + "\n})();", data), cls);
    }

    public <T> T eval(String script, Map<String,Object> data){
        return eval((String)null,script,data);
    }

    /**
     * Eval script
     *
     * @param script source
     * @param data context
     * @param <T>
     * @return last script statement result
     * @throws org.s1.script.errors.ScriptException
     * @throws org.s1.script.errors.ScriptLimitException
     * @throws org.s1.script.errors.SyntaxException
     */
    public <T> T eval(String name, final String script, Map<String,Object> data) throws ScriptException,ScriptLimitException,SyntaxException {
        long t = System.currentTimeMillis();
        if(data==null)
            data = Objects.newHashMap();
        if(script.length()>sizeLimit)
            throw new ScriptLimitException(ScriptLimitException.Limits.SIZE,getSizeLimit());
        if(LOG.isDebugEnabled()){
            LOG.debug("Evaluating S1 script:\n"+script+"\nWith data:"+data);
        }

        AstRoot _root = null;
        if(Objects.isNullOrEmpty(name)){
            _root = parseScript(script);
        }else{
            _root = astCache.get(name,new Closure<String, AstRoot>() {
                @Override
                public AstRoot call(String input) {
                    return parseScript(script);
                }
            });
        }
        final AstRoot root = _root;

        final Context ctx=new Context(getMemoryLimit());
        ctx.getVariables().putAll(data);

        //functions from options
        for(final Map<String,Object> f:functions){
            String clName = Objects.get(f,"class");
            String ns = Objects.get(f,"namespace","");
            if(ns.length()>0 && !ns.endsWith("."))
                ns+=".";
            Class<? extends ScriptFunctionSet> cls = null;
            try {
                cls = (Class<? extends ScriptFunctionSet>)Class.forName(clName);
                addFunctions(ns, ctx, cls);
            } catch (Throwable e) {
                LOG.warn("Class "+cls+" cannot be initialized as ScriptFunctions: "+e.getMessage(),e);
            }
        }

        //system functions
        addFunctions(SYSTEM_FUNCTION_NS+".", ctx, SystemFunctionSet.class);
        final Session.SessionBean sb = Session.getSessionBean();
        Future<Object> f = null;
        if(sb!=null){
            //run script
            f = service.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    String id = null;
                    try{
                        id = Session.start(sb.getId());
                        ASTEvaluator ast = new ASTEvaluator(script);
                        Object o = ast.eval(root,ctx);
                        return o;
                    }finally {
                        Session.end(id);
                    }
                }
            });
        }else{
            //run script
            f = service.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    ASTEvaluator ast = new ASTEvaluator(script);
                    Object o = ast.eval(root,ctx);
                    return o;
                }
            });
        }

        try {
            long l = System.currentTimeMillis();
            T result = null;
            /*try {
                // Try to get answer in short timeout, should be available
                result = (T)f.get(getTimeLimit(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException te) {
                f.cancel(true);
                throw new ScriptLimitException(ScriptLimitException.Limits.TIME,getTimeLimit());
            }*/
            while(true){
                if(System.currentTimeMillis()-l>getTimeLimit()){
                    f.cancel(true);
                    throw new ScriptLimitException(ScriptLimitException.Limits.TIME,getTimeLimit());
                }

                if(f.isDone()){
                    break;
                }
                Thread.sleep(1);
            }
            result = (T)f.get();

            if(LOG.isDebugEnabled()){
                LOG.debug("Script result ("+(System.currentTimeMillis()-t)+"ms.): "+result);
            }
            return result;
        } catch (InterruptedException e){
            throw S1SystemError.wrap(e);
        } catch (ExecutionException e){
            if(e.getCause()!=null){
                if(e.getCause() instanceof ScriptLimitException)
                    throw (ScriptLimitException)e.getCause();
                if(e.getCause() instanceof ScriptException)
                    throw (ScriptException)e.getCause();
            }
            throw S1SystemError.wrap(e);
        }
    }

    protected AstRoot parseScript(String script){
        CompilerEnvirons ce = new CompilerEnvirons();
        ce.setRecordingComments(false);
        ce.setStrictMode(true);
        ce.setXmlAvailable(false);

        final AstRoot root;
        try {
            root = new Parser(ce).parse(script,"S1RestrictedScript",1);
        } catch (Throwable e) {
            String message = "";
            if(e instanceof EvaluatorException){
                EvaluatorException ex = (EvaluatorException)e;
                message = "Syntax error: "+ASTEvaluator.getErrorMessage(script,ex.getLineNumber()-1,ex.getColumnNumber());
            }else{
                message = e.getMessage();
            }
            throw new SyntaxException(message,e);
        }
        if(LOG.isDebugEnabled()){
            LOG.debug(root.debugPrint());
        }
        return root;
    }

    /**
     * System functions namespace
     */
    public static final String SYSTEM_FUNCTION_NS = "s1";

    /**
     *
     * @param ns
     * @param c
     * @param cls
     */
    private void addFunctions(final String ns, Context c, final Class<? extends ScriptFunctionSet> cls){
        for(final Method method:cls.getDeclaredMethods()){
            if(!Modifier.isPublic(method.getModifiers())){
                continue;
            }
            if(LOG.isTraceEnabled()){
                LOG.trace(ns + method.getName() + " -> " + cls.getName() + "#" + method.getName());
            }
            Objects.set(c.getVariables(), ns+method.getName(), new ScriptFunction(new Context(getMemoryLimit()), Objects.newArrayList(String.class)) {
                @Override
                public Object call() throws ScriptException {
                    List args = getContext().get("arguments");
                    try {
                        for(int i=0;i<method.getParameterTypes().length;i++){
                            if(args.size()>i){
                                args.set(i,Objects.cast(args.get(i),method.getParameterTypes()[i]));
                            }
                        }
                        if(LOG.isTraceEnabled()){
                            LOG.trace(ns+method.getName()+"("+args+")");
                        }
                        ScriptFunctionSet sf = cls.newInstance();
                        sf.setContext(getContext());
                        return method.invoke(sf, args.toArray());
                    } catch (Throwable e) {
                        throw new ScriptException(ns+method.getName()+": "+e.getMessage(), e);
                    }
                }
            });
        }
    }

    public void invalidateCache(String name){
        astCache.invalidate(name);
        templateCache.invalidate(name);
    }

    /**
     * Function name for printing
     */
    public static final String TEMPLATE_PRINT_FUNCTION = "_print";
    public static final String TEMPLATE_START_CUSTOM_FUNCTION = "startPrint";
    public static final String TEMPLATE_END_CUSTOM_FUNCTION = "endPrint";

    protected static final String startExpr="<%=";
    protected static final String endExpr="%>";
    protected static final String startCode = "<%";
    protected static final String endCode = "%>";

    /**
     * Run template
     *
     * @param template template text
     * @param data context
     * @return evaluated template
     * @throws ScriptException
     * @throws ScriptLimitException
     * @throws SyntaxException
     */
    public String template(String template, Map<String,Object> data)
            throws ScriptException,ScriptLimitException,SyntaxException{
        return template(null,template,data);
    }

    public String template(String name, String template, Map<String,Object> data)
        throws ScriptException,ScriptLimitException,SyntaxException{

        if(LOG.isDebugEnabled()){
            LOG.debug("Building S1 template "+name+":\n"+template+"\nWith data:"+data);
        }
        if(Objects.isNullOrEmpty(name)){
            template = parseTemplate(template);
        }else{
            final String t = template;
            template = templateCache.get(name,new Closure<String, String>() {
                @Override
                public String call(String input) {
                    return parseTemplate(t);
                }
            });
        }

        //eval
        final Map<String,StringBuilder> customPrintMap = Objects.newHashMap();
        final List<String> customPrintStack = Objects.newArrayList();

        final StringBuilder printBuffer = new StringBuilder();
        if(data==null){
            data = Objects.newHashMap();
        }
        final Map<String,Object> _data = data;
        data.put(TEMPLATE_PRINT_FUNCTION, new ScriptFunction(new Context(getMemoryLimit()),Objects.newArrayList("text")) {
            @Override
            public Object call() throws ScriptException {
                List<Object> args = getContext().get("arguments");
                StringBuilder sb = printBuffer;
                String name = null;
                if(customPrintStack.size()>0)
                    name = customPrintStack.get(customPrintStack.size()-1);
                if(!Objects.isNullOrEmpty(name)){
                    sb = customPrintMap.get(name);
                }
                for(Object o:args){
                    sb.append(o);
                }
                return null;
            }
        });
        data.put(TEMPLATE_START_CUSTOM_FUNCTION, new ScriptFunction(new Context(getMemoryLimit()), Objects.newArrayList("name")) {
            @Override
            public Object call() throws ScriptException {
                String name = getContext().get(String.class, "name");
                customPrintMap.put(name, new StringBuilder());
                customPrintStack.add(name);
                return null;
            }
        });
        data.put(TEMPLATE_END_CUSTOM_FUNCTION, new ScriptFunction(new Context(getMemoryLimit()),Objects.newArrayList(String.class)) {
            @Override
            public Object call() throws ScriptException {
                String name = null;
                if(customPrintStack.size()>0) {
                    name = customPrintStack.get(customPrintStack.size() - 1);
                    customPrintStack.remove(customPrintStack.size() - 1);
                }
                if(!Objects.isNullOrEmpty(name)){
                    String text = "";
                    StringBuilder sb = customPrintMap.get(name);
                    customPrintMap.remove(name);
                    if(sb!=null){
                        text = sb.toString();
                    }
                    Object o = _data.get(name);
                    if(o==null){
                        throw new ScriptException("Custom print function "+name+" is not defined");
                    }
                    if(o instanceof ScriptFunction){
                        ScriptFunction sf = ((ScriptFunction)o);
                        List<String> params = Objects.newArrayList(text);
                        sf.getContext().getVariables().put("text",text);
                        sf.getContext().getVariables().put("arguments",params);

                        StringBuilder sb2 = printBuffer;
                        String name2 = null;
                        if(customPrintStack.size()>0)
                            name2 = customPrintStack.get(customPrintStack.size()-1);
                        if(!Objects.isNullOrEmpty(name2)){
                            sb2 = customPrintMap.get(name2);
                        }

                        sb2.append(sf.call());
                    }else{
                        throw new ScriptException("Custom print function "+name+" is not instanceof ScriptFunction");
                    }
                }
                return null;
            }
        });

        eval(name, template, data);

        template = printBuffer.toString();
        template = template
                .replace("&startCode;",startCode)
                .replace("&endCode;",endCode)
                .replace("&startExpr;",startExpr)
                .replace("&endExpr;",endExpr)
                .replace("&amp;","&");
        return template;
    }

    protected String parseTemplate(String template){
        template = template
                .replace("&","&amp;")
                .replace("\\"+startCode+"\\","&startCode;")
                .replace("\\"+endCode+"\\","&endCode;")
                .replace("\\"+startExpr+"\\","&startExpr;")
                .replace("\\"+endExpr+"\\","&endExpr;");

        final String BEGIN = "|--"+ UUID.randomUUID().toString() +"--|";
        final String END = "|--"+ UUID.randomUUID().toString() +"--|";

        final Pattern codeP = Pattern.compile(Pattern.quote(startCode)+"(.+?)"+Pattern.quote(endCode),Pattern.DOTALL);
        final Pattern exprP = Pattern.compile(Pattern.quote(startExpr)+"(.+?)"+Pattern.quote(endExpr),Pattern.DOTALL);
        final Pattern textP = Pattern.compile(Pattern.quote(END)+"(.+?)"+Pattern.quote(BEGIN),Pattern.DOTALL);

        //expr
        final Matcher matcherExpr = exprP.matcher(template);
        while (matcherExpr.find()) {
            String text = matcherExpr.group(1);
            template = template.replace(startExpr+text+endExpr,
                    BEGIN+TEMPLATE_PRINT_FUNCTION+"("+text+");"+END);
        }

        //code
        final Matcher matcherCode = codeP.matcher(template);
        while (matcherCode.find()) {
            String text = matcherCode.group(1);
            template = template.replace(startCode+text+endCode,
                    BEGIN+"\n"+text+"\n"+END);
        }

        //text
        int s = template.indexOf(BEGIN);
        if(s==-1){
            template = printText(template);
        }else{
            if(s>=0){
                String text = template.substring(0,s);
                template = printText(text)+template.substring(s+BEGIN.length());
            }
            int e = template.lastIndexOf(END);
            if(e>0 && e<template.length()-1){
                String text = template.substring(e+END.length());
                template = template.substring(0,e)+printText(text);
            }

            final Matcher matcherText = textP.matcher(template);
            while (matcherText.find()) {
                String text = matcherText.group(1);
                template = template.replace(END+text+BEGIN,printText(text));
            }
        }
        return template;
    }

    /**
     *
     * @param text
     * @return
     */
    private String printText(String text){
        return TEMPLATE_PRINT_FUNCTION+"(\""+text.replace("\"","\\\"")
                .replaceAll("(\r\n|\n|\n\r)","\\\\n\",\n\"")+"\");";
    }

}
