package org.s1.script;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;
import org.s1.S1SystemError;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.s1.options.OptionsStorage;
import org.s1.script.functions.ScriptFunctions;
import org.s1.script.functions.ScriptSystemFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S1 Script engine
 */
public class S1ScriptEngine {

    private static final Logger LOG = LoggerFactory.getLogger(S1ScriptEngine.class);

    private List<Map<String,Object>> functions = Objects.newArrayList();
    private long timeLimit = 0;
    private long sizeLimit = 0;
    private long memoryLimit = 0;

    /**
     * Create with path='scriptEngine'
     */
    public S1ScriptEngine(){
        this("scriptEngine");
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
        return Objects.cast(evalInFunction(script,data),cls);
    }

    /**
     *
     * @param script
     * @param data
     * @param <T>
     * @return
     */
    public <T> T evalInFunction(String script, Map<String,Object> data){
        return eval("(function(){"+script+"\n})();",data);
    }

    /**
     * Eval script and cast result
     *
     * @param cls
     * @param script
     * @param data
     * @param <T>
     * @return
     */
    public <T> T eval(Class<T> cls, String script, Map<String,Object> data){
        return Objects.cast(eval(script,data),cls);
    }

    /**
     * Eval script
     *
     * @param script source
     * @param data context
     * @param <T>
     * @return last script statement result
     * @throws ScriptException
     * @throws ScriptLimitException
     * @throws SyntaxException
     */
    public <T> T eval(String script, Map<String,Object> data) throws ScriptException,ScriptLimitException,SyntaxException{
        long t = System.currentTimeMillis();
        if(data==null)
            data = Objects.newHashMap();
        if(script.length()>sizeLimit)
            throw new ScriptLimitException(ScriptLimitException.Limits.SIZE,getSizeLimit());
        if(LOG.isDebugEnabled()){
            LOG.debug("Evaluating S1 script:\n"+script+"\nWith data:"+data);
        }
        CompilerEnvirons ce = new CompilerEnvirons();
        ce.setRecordingComments(false);
        ce.setStrictMode(true);
        ce.setXmlAvailable(false);

        final AstRoot root;
        try {
            root = new Parser(ce).parse(script,"S1RestrictedScript",1);
        } catch (Throwable e) {
            throw new SyntaxException(e.getMessage(),e);
        }
        if(LOG.isDebugEnabled()){
            LOG.debug(root.debugPrint());
        }

        final Context ctx=new Context(getMemoryLimit());
        ctx.getVariables().putAll(data);

        //functions from options
        for(final Map<String,Object> f:functions){
            String clName = Objects.get(f,"class");
            String ns = Objects.get(f,"namespace","");
            if(ns.length()>0 && !ns.endsWith("."))
                ns+=".";
            Class<? extends ScriptFunctions> cls = null;
            try {
                cls = (Class<? extends ScriptFunctions>)Class.forName(clName);
                addFunctions(ns, ctx, cls);
            } catch (Throwable e) {
                LOG.warn("Class "+cls+" cannot be initialized as ScriptFunctions: "+e.getMessage(),e);
            }
        }

        //system functions
        addFunctions(SYSTEM_FUNCTION_NS+".", ctx, ScriptSystemFunctions.class);

        //run script
        Future<Object> f = executeTask(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return new ASTEvaluator().eval(root,ctx);
            }
        },getTimeLimit());

        try {
            T result = (T)f.get();
            if(LOG.isDebugEnabled()){
                LOG.debug("Script result ("+(System.currentTimeMillis()-t)+"ms.): "+result);
            }
            return result;
        } catch (CancellationException e){
            throw new ScriptLimitException(ScriptLimitException.Limits.TIME,getTimeLimit());
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

    /**
     *
     * @param c
     * @param timeoutMS
     * @param <T>
     * @return
     */
    private <T> Future<T> executeTask(Callable<T> c, long timeoutMS){
        ExecutorService service = Executors.newFixedThreadPool(1);
        ScheduledExecutorService canceller = Executors.newSingleThreadScheduledExecutor();

        final Future<T> future = service.submit(c);
        canceller.schedule(new Callable<Void>(){
            public Void call(){
                future.cancel(true);
                return null;
            }
        }, timeoutMS, TimeUnit.MILLISECONDS);
        return future;
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
    private void addFunctions(final String ns, Context c, final Class<? extends ScriptFunctions> cls){
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
                        ScriptFunctions sf = cls.newInstance();
                        sf.setContext(getContext());
                        return method.invoke(sf, args.toArray());
                    } catch (Throwable e) {
                        throw new ScriptException(ns+method.getName()+": "+e.getMessage(), e);
                    }
                }
            });
        }
    }

    /**
     * Function name for printing
     */
    public static final String TEMPLATE_PRINT_FUNCTION = "_print";

    /**
     * Run template
     *
     * @param template template text
     * @param data context
     * @param startExpr tag for expression start
     * @param endExpr tag for expression end
     * @param startCode tag for code start
     * @param endCode tag for code end
     * @return evaluated template
     * @throws ScriptException
     * @throws ScriptLimitException
     * @throws SyntaxException
     */
    public String template(String template, Map<String,Object> data, String startExpr, String endExpr, String startCode, String endCode)
        throws ScriptException,ScriptLimitException,SyntaxException{

        if(LOG.isDebugEnabled()){
            LOG.debug("Building S1 template:\n"+template+"\nWith data:"+data+"\n"
                    +startExpr+"expression"+endExpr
                    +startCode+"code"+endCode);
        }

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
            template = template.replace(startExpr+matcherExpr.group(1)+endExpr,
                    BEGIN+TEMPLATE_PRINT_FUNCTION+"("+text+");"+END);
        }

        //code
        final Matcher matcherCode = codeP.matcher(template);
        while (matcherCode.find()) {
            String text = matcherCode.group(1);
            template = template.replace(startCode+matcherCode.group(1)+endCode,
                    BEGIN+"\n"+matcherCode.group(1)+"\n"+END);
        }

        //text
        int s = template.indexOf(BEGIN);
        if(s==-1){
            template = printText(template);
        }else{
            if(s>0){
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

        //eval
        final StringBuilder sb = new StringBuilder();
        if(data==null){
            data = Objects.newHashMap();
        }
        data.put(TEMPLATE_PRINT_FUNCTION, new ScriptFunction(new Context(getMemoryLimit()),Objects.newArrayList("text")) {
            @Override
            public Object call() throws ScriptException {
                //String text = getContext().get(String.class,"text");
                //sb.append(text);
                List<Object> args = getContext().get("arguments");
                for(Object o:args){
                    sb.append(o);
                }
                return null;
            }
        });
        eval(template,data);

        template = sb.toString();
        template = template
                .replace("&startCode;",startCode)
                .replace("&endCode;",endCode)
                .replace("&startExpr;",startExpr)
                .replace("&endExpr;",endExpr)
                .replace("&amp;","&");
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
