package org.s1.script;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;
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

/**
 * s1v2
 * User: GPykhov
 * Date: 06.02.14
 * Time: 16:07
 */
public class S1ScriptEngine {

    private static final Logger LOG = LoggerFactory.getLogger(S1ScriptEngine.class);

    private Map<String,String> functions = Objects.newHashMap();

    public S1ScriptEngine(){
        this("scriptEngine.functions");
    }

    public S1ScriptEngine(String path){
        this(OptionsStorage.SYSTEM_PROPERTIES,path);
    }

    public S1ScriptEngine(String options, String path){
        functions = Options.getStorage().get(options,path,Objects.newHashMap(String.class,String.class));
    }

    public Map<String, String> getFunctions() {
        return functions;
    }

    public void setFunctions(Map<String, String> functions) {
        this.functions = functions;
    }

    public Object eval(String script, Map<String,Object> data){
        CompilerEnvirons ce = new CompilerEnvirons();
        ce.setRecordingComments(false);
        ce.setStrictMode(true);
        ce.setXmlAvailable(false);

        AstRoot ar = new Parser(ce).parse(script,"S1RestrictedScript",1);

        Context c=new Context();
        c.getVariables().putAll(data);

        //functions from options
        for(final String k:functions.keySet()){
            String f = functions.get(k);
            Class<? extends ScriptFunctions> cls = null;
            try {
                cls = (Class<? extends ScriptFunctions>)Class.forName(f);
            } catch (Throwable e) {
                LOG.warn("Class "+cls+" cannot be initialized as ScriptFunctions: "+e.getMessage());
            }
            addFunctions("", c, cls);
        }

        //system functions
        addFunctions(SYSTEM_FUNCTION_NS+".", c, ScriptSystemFunctions.class);

        return ASTEvaluator.eval(ar,c);
    }

    public static final String SYSTEM_FUNCTION_NS = "s1";

    private void addFunctions(final String ns, Context c, final Class<? extends ScriptFunctions> cls){
        for(final Method method:cls.getDeclaredMethods()){
            if(!Modifier.isPublic(method.getModifiers())){
                continue;
            }
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

    public String template(String template, Map<String,Object> data){
        return "";
    }

}
