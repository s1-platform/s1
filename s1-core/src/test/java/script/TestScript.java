package script;

import org.s1.S1SystemError;
import org.s1.cluster.Session;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.misc.FileUtils;
import org.s1.objects.Objects;
import org.s1.script.*;
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
                new ScriptFunction(new Context(1000), Objects.newArrayList(String.class,"message","b")) {
                    @Override
                    public Object call() throws ScriptException {
                        Object o = getContext().getVariables().get("b");
                        boolean b = false;
                        if(o instanceof Boolean)
                            b = ((Boolean) o).booleanValue();
                        else
                            b = !Objects.isNullOrEmpty(o);
                        if(!b)
                            throw new ScriptException(getContext().getVariables().get("message"));
                        return null;
                    }
                },
                "print",
                new ScriptFunction(new Context(1000), Objects.newArrayList(String.class,"message")) {
                    @Override
                    public Object call() throws ScriptException {
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
                Session.run("script_"+input,new Closure<String, Object>() {
                    @Override
                    public Object call(String input) throws ClosureException {
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
                });
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

    public void testEvalInFunction(){
        final File dir = new File(getTestClassesHome()+"/script/templates");
        int p=1;
        title("Eval in function, parallel: "+p);
        final S1ScriptEngine scriptEngine = new S1ScriptEngine();
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                assertEquals(10,scriptEngine.evalInFunction(Integer.class,"return 10;",null).intValue());
                assertTrue(scriptEngine.evalInFunction(Boolean.class,"return true;",null));
                assertFalse(scriptEngine.evalInFunction(Boolean.class,"return false;",null));
                return null;
            }
        }));
    }

    public void testTimeLimit(){
        int p=1;
        title("Time limit test, parallel: "+p);
        final S1ScriptEngine scriptEngine = new S1ScriptEngine();
        scriptEngine.setTimeLimit(2000);
        final Map<String,Object> data = Objects.newHashMap(
                "sleep",
                new ScriptFunction(new Context(1000), Objects.newArrayList(String.class,"time")) {
                    @Override
                    public Object call() throws ScriptException {
                        sleep(getContext().get(Long.class, "time"));
                        return null;
                    }
                }
        );
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                String t = scriptEngine.eval("'test'+'1';", data);
                assertEquals("test1", t);

                //timeout
                boolean b = false;
                try {
                    scriptEngine.eval("sleep(3000); 'test1'", data);
                } catch (ScriptLimitException e){
                    b = e.getType()== ScriptLimitException.Limits.TIME;
                }
                assertTrue(b);

                //timeout 2
                b = false;
                try {
                    scriptEngine.eval("while(true){}", data);
                } catch (ScriptLimitException e){
                    b = e.getType()== ScriptLimitException.Limits.TIME;
                }
                assertTrue(b);
                return null;
            }
        }));
    }

    public void testSizeLimit(){
        int p=1;
        title("Size limit test, parallel: "+p);
        final S1ScriptEngine scriptEngine = new S1ScriptEngine();
        scriptEngine.setSizeLimit(10);

        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                String t = scriptEngine.eval("'012345'", null);
                assertEquals("012345", t);

                //limit
                boolean b = false;
                try {
                    scriptEngine.eval("'1234567890'", null);
                } catch (ScriptLimitException e){
                    b = e.getType()== ScriptLimitException.Limits.SIZE;
                }
                assertTrue(b);
                return null;
            }
        }));
    }

    public void testMemoryLimit(){
        int t = Thread.activeCount();
        int p=10;
        title("Memory limit test, parallel: "+p);
        final S1ScriptEngine scriptEngine = new S1ScriptEngine();
        scriptEngine.setMemoryLimit(1000);

        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                String t = scriptEngine.eval("'012345'", null);
                assertEquals("012345", t);

                //limit
                boolean b = false;
                try {
                    scriptEngine.eval("var a = '1234567890';while(true){a+=a;}", null);
                } catch (ScriptLimitException e){
                    b = e.getType()== ScriptLimitException.Limits.MEMORY;
                }
                assertTrue(b);


                //limit 2
                b = false;
                try {
                    scriptEngine.eval("var arr = []; while(true){s1.add(arr,'asd');}", null);
                } catch (ScriptLimitException e){
                    b = e.getType()== ScriptLimitException.Limits.MEMORY;
                }
                assertTrue(b);


                //limit 3
                b = false;
                try {
                    scriptEngine.eval("var obj = {a:'asd'}; while(true){s1.set(obj,'a',obj.a+'qwer');}", null);
                } catch (ScriptLimitException e){
                    b = e.getType()== ScriptLimitException.Limits.MEMORY;
                }
                assertTrue(b);

                return null;
            }
        }));
        trace(Thread.activeCount());
        assertEquals(t,Thread.activeCount());
    }

    public void testSyntaxError(){
        title("Syntax error");
        final S1ScriptEngine scriptEngine = new S1ScriptEngine();
        boolean b = false;
        try{
            scriptEngine.eval("return;",null);
        }catch (SyntaxException e){
            b = true;
            trace(e.getMessage());
        }
        assertTrue(b);

    }

    public void testOptionsFunctions(){
        title("Options functions");
        final S1ScriptEngine scriptEngine = new S1ScriptEngine();
        assertTrue(Objects.equals(5,scriptEngine.eval("test.sum(2,3);",null)));

    }

}