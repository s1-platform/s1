package script;

import org.s1.S1SystemError;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.misc.FileUtils;
import org.s1.objects.Objects;
import org.s1.script.Context;
import org.s1.script.JavaScriptException;
import org.s1.script.S1ScriptEngine;
import org.s1.script.ScriptFunction;
import org.s1.test.BasicTest;
import org.s1.test.LoadTestUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 06.02.14
 * Time: 16:03
 */
public class TestScript extends BasicTest{

    public void testCases(){
        final File dir = new File(getTestClassesHome()+"/script");
        int p=1;
        final S1ScriptEngine scriptEngine = new S1ScriptEngine();
        final Map<String,Object> data = Objects.newHashMap(
                "assert",
                new ScriptFunction(new Context(), Objects.newArrayList(String.class,"message","b")) {
                    @Override
                    public Object call() throws JavaScriptException {
                        Object o = getContext().getVariables().get("b");
                        boolean b = false;
                        if(o instanceof Boolean)
                            b = ((Boolean) o).booleanValue();
                        else
                            b = !Objects.isNullOrEmpty(o);
                        if(!b)
                            throw new JavaScriptException(getContext().getVariables().get("message"));
                        return null;
                    }
                },
                "print",
                new ScriptFunction(new Context(), Objects.newArrayList(String.class,"message")) {
                    @Override
                    public Object call() throws JavaScriptException {
                        Object o = getContext().getVariables().get("message");
                        if(o!=null)
                            System.out.println(">>>"+o.getClass().getName()+":"+o);
                        else
                            System.out.println(">>>null");
                        return null;
                    }
                }
        );
        title("Cases test, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                File[] fs = dir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return !f.isDirectory() && f.getName().endsWith(".js");
                    }
                });
                for(File f:fs){
                    String script = null;
                    try {
                        script = FileUtils.readFileToString(f,"UTF-8");
                    } catch (IOException e) {
                        throw S1SystemError.wrap(e);
                    }
                    try{
                        scriptEngine.eval(script,data);
                    }catch (RuntimeException e){
                        throw new RuntimeException(f.getName()+":"+e.getMessage(),e);
                    }
                }
                return null;
            }
        }));
    }

    public void testTemplate(){
        final File dir = new File(getTestClassesHome()+"/script/templates");
        int p=1;
        title("Template test, parallel: "+p);
        final S1ScriptEngine scriptEngine = new S1ScriptEngine();
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                File[] fs = dir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return !f.isDirectory() && f.getName().endsWith(".tpl");
                    }
                });
                for(File f:fs){
                    String template = null;
                    try {
                        template = FileUtils.readFileToString(f,"UTF-8");
                    } catch (IOException e) {
                        throw S1SystemError.wrap(e);
                    }
                    try{
                        String t = scriptEngine.template(template,null,"{{","}}","<%","%>");
                        if(input==0){
                            //trace(f.getName()+":\nINPUT:\n"+template+"\nOUTPUT:\n"+t);
                        }
                    }catch (RuntimeException e){
                        throw new RuntimeException(f.getName()+":"+e.getMessage(),e);
                    }
                }
                return null;
            }
        }));
    }

}
