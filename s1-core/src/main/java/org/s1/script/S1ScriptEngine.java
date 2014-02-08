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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * s1v2
 * User: GPykhov
 * Date: 06.02.14
 * Time: 16:07
 */
public class S1ScriptEngine {

    private static final Logger LOG = LoggerFactory.getLogger(S1ScriptEngine.class);

    private Map<String,String> functions = Objects.newHashMap();
    private long timeLimit = 0;
    private long sizeLimit = 0;
    private long memoryLimit = 0;

    public S1ScriptEngine(){
        this("scriptEngine");
    }

    public S1ScriptEngine(String path){
        this(OptionsStorage.SYSTEM_PROPERTIES,path);
    }

    public S1ScriptEngine(String options, String path){
        functions = Options.getStorage().get(options,path+".functions",Objects.newHashMap(String.class,String.class));
        timeLimit = Options.getStorage().get(options,path+".timeLimit",30000);
        memoryLimit = Options.getStorage().get(options,path+".memoryLimit",16*1024*1024);
        sizeLimit = Options.getStorage().get(options,path+".sizeLimit",16*1024*1024);
    }

    public Map<String, String> getFunctions() {
        return functions;
    }

    public void setFunctions(Map<String, String> functions) {
        this.functions = functions;
    }

    public long getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(long timeLimit) {
        this.timeLimit = timeLimit;
    }

    public long getSizeLimit() {
        return sizeLimit;
    }

    public void setSizeLimit(long sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    public long getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(long memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public <T> T eval(Class<T> cls, String script, Map<String,Object> data){
        return Objects.cast(eval(script,data),cls);
    }

    public <T> T eval(String script, Map<String,Object> data){
        if(data==null)
            data = Objects.newHashMap();
        if(script.length()>sizeLimit)
            throw new SizeLimitException("Limit: "+sizeLimit+" chars");
        CompilerEnvirons ce = new CompilerEnvirons();
        ce.setRecordingComments(false);
        ce.setStrictMode(true);
        ce.setXmlAvailable(false);

        final AstRoot root = new Parser(ce).parse(script,"S1RestrictedScript",1);

        final Context ctx=new Context();
        ctx.getVariables().putAll(data);

        //functions from options
        for(final String k:functions.keySet()){
            String f = functions.get(k);
            Class<? extends ScriptFunctions> cls = null;
            try {
                cls = (Class<? extends ScriptFunctions>)Class.forName(f);
            } catch (Throwable e) {
                LOG.warn("Class "+cls+" cannot be initialized as ScriptFunctions: "+e.getMessage());
            }
            addFunctions("", ctx, cls);
        }

        //system functions
        addFunctions(SYSTEM_FUNCTION_NS+".", ctx, ScriptSystemFunctions.class);

        //run script
        Future<Object> f = executeTask(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                //return null;
                return ASTEvaluator.eval(root,ctx,new MemoryHeap(memoryLimit));
            }
        },timeLimit);

        try {
            return (T)f.get();
        } catch (CancellationException e){
            throw new TimeLimitException("Limit: "+timeLimit+" ms.");
        } catch (InterruptedException e){
            throw S1SystemError.wrap(e);
        } catch (ExecutionException e){
            if(e.getCause()!=null && e.getCause() instanceof MemoryLimitException)
                throw new MemoryLimitException(e.getCause().getMessage(),e.getCause().getCause());
            throw S1SystemError.wrap(e);
        }

        //return ASTEvaluator.eval(ar,c);
    }

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

    public static final String SYSTEM_FUNCTION_NS = "s1";

    private void addFunctions(final String ns, Context c, final Class<? extends ScriptFunctions> cls){
        //System.out.println("-------------");
        for(final Method method:cls.getDeclaredMethods()){
            if(!Modifier.isPublic(method.getModifiers())){
                continue;
            }
            //System.out.println("* "+method.getName());
            Objects.set(c.getVariables(), ns+method.getName(), new ScriptFunction(new Context(), Objects.newArrayList(String.class)) {
                @Override
                public Object call() throws JavaScriptException {
                    List args = getContext().get("arguments");
                    try {
                        for(int i=0;i<method.getParameterTypes().length;i++){
                            if(args.size()>i){
                                args.set(i,Objects.cast(args.get(i),method.getParameterTypes()[i]));
                            }
                        }
                        System.out.println(ns+method.getName()+": "+args);
                        ScriptFunctions sf = cls.newInstance();
                        sf.setContext(getContext());
                        return method.invoke(sf, args.toArray());
                    } catch (Throwable e) {
                        throw new JavaScriptException(ns+method.getName()+": "+e.getMessage(), e);
                    }
                }
            });
        }
    }

    public static final String TEMPLATE_PRINT_FUNCTION = "_print";

    public String template(String template, Map<String,Object> data, String startExpr, String endExpr, String startCode, String endCode){

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
        data.put(TEMPLATE_PRINT_FUNCTION, new ScriptFunction(new Context(),Objects.newArrayList("text")) {
            @Override
            public Object call() throws JavaScriptException {
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

    private String printText(String text){
        //return TEMPLATE_PRINT_FUNCTION+"(\""+text.replace("\"","\\\"")
        //        .replaceAll("(\r\n|\n|\n\r)","\\\\n\"+\n\"")+"\");";
        return TEMPLATE_PRINT_FUNCTION+"(\""+text.replace("\"","\\\"")
                .replaceAll("(\r\n|\n|\n\r)","\\\\n\",\n\"")+"\");";
    }

}
