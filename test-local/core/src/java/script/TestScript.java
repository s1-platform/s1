package script;

import org.s1.cluster.Session;
import org.s1.misc.FileUtils;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.s1.script.Context;
import org.s1.script.S1ScriptEngine;
import org.s1.script.errors.ScriptException;
import org.s1.script.errors.ScriptLimitException;
import org.s1.script.errors.SyntaxException;
import org.s1.script.function.CustomPrintFunction;
import org.s1.script.function.ScriptFunction;
import org.s1.testing.BasicTest;
import org.s1.testing.LoadTestUtils;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * s1v2
 * User: GPykhov
 * Date: 06.02.14
 * Time: 16:03
 */
public class TestScript extends BasicTest{

    @Test
    public void testCases(){
        int p=10;
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
                            throw new ScriptException("Assertion failed: " +getContext().getVariables().get("message"));
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

        assertEquals(p, LoadTestUtils.run("test",p,p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {
                try{
                    Session.start("script_"+input);
                    for(String f:FileUtils.getClasspathResources("/script")){
                        if(f.contains("/") || !f.endsWith(".js"))
                            continue;
                        String script = resourceAsString("/script/"+f);
                        try{
                            scriptEngine.eval(script,data);
                        }catch (RuntimeException e){
                            throw new RuntimeException(f+":"+e.getMessage(),e);
                        }
                    }
                }finally {
                    Session.end("script_"+input);
                }
            }
        }));
    }

    @Test
    public void testTemplate(){
        int p=10;

        final S1ScriptEngine scriptEngine = new S1ScriptEngine();
        assertEquals(p, LoadTestUtils.run("test",p,p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {
                for(String f:FileUtils.getClasspathResources("/script")){
                    if(f.contains("/") || !f.endsWith(".tpl"))
                        continue;
                    String template = resourceAsString("/script/"+f);
                    try{
                        String t = scriptEngine.template(template,null);
                        if(input==0){
                            //trace(f.getName()+":\nINPUT:\n"+template+"\nOUTPUT:\n"+t);
                        }
                    }catch (RuntimeException e){
                        throw new RuntimeException(f+":"+e.getMessage(),e);
                    }
                }

            }
        }));
    }

    @Test
    public void testCustomPrintTemplate(){
        int p=10;

        final S1ScriptEngine scriptEngine = new S1ScriptEngine();
        assertEquals(p, LoadTestUtils.run("test",p,p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {

                String t = scriptEngine.template("test" +
                        "<%startPrint('test1');%>" +
                        "qwer" +
                        "<%endPrint();%>" +
                        "zxcv",Objects.newSOHashMap(
                        "test1",new CustomPrintFunction(new Context(10000)) {
                            @Override
                            public String print(String text) throws ScriptException {
                                return text+"...";
                            }
                        }
                ));
                if(input==0){
                    trace(t);
                }
                assertEquals("testqwer...zxcv",t);


                t = scriptEngine.template("test" +
                        "<%startPrint('test1');%>" +
                        "111" +
                        "<%startPrint('test2');%>" +
                        "qwer" +
                        "<%endPrint();%>" +
                        "222" +
                        "<%endPrint();%>" +
                        "zxcv",Objects.newSOHashMap(
                        "test1",new CustomPrintFunction(new Context(10000)) {
                            @Override
                            public String print(String text) throws ScriptException {
                                return text+"...";
                            }
                        },
                        "test2",new CustomPrintFunction(new Context(10000)) {
                            @Override
                            public String print(String text) throws ScriptException {
                                return "/"+text+"/";
                            }
                        }
                ));
                if(input==0){
                    trace(t);
                }
                assertEquals("test111/qwer/222...zxcv",t);

                boolean b = false;
                try{
                    scriptEngine.template("test" +
                            "<%startPrint('test1');%>" +
                            "qwer" +
                            "<%endPrint();%>" +
                            "zxcv",null
                    );
                }catch (ScriptException e){
                    if (input==0)
                        trace(e.getMessage());
                    b = true;
                }
                assertTrue(b);

                b = false;
                try{
                    scriptEngine.template("test" +
                                    "<%startPrint('test1');%>" +
                                    "qwer" +
                                    "<%endPrint();%>" +
                                    "zxcv",Objects.newSOHashMap("test1","qwer")
                    );
                }catch (ScriptException e){
                    if (input==0)
                        trace(e.getMessage());
                    b = true;
                }
                assertTrue(b);

            }
        }));
    }

    @Test
    public void testTemplateError(){
        int p=10;

        final S1ScriptEngine scriptEngine = new S1ScriptEngine();
        boolean b = false;
        try{
            scriptEngine.template("hello \n" +
                    "<%var a=b();%>",null);
        }catch (ScriptException e){
            trace(e);
            assertTrue(e.getMessage().contains("var a=b()"));
            b = true;
        }
        assertTrue(b);
        b = false;
        try{
            scriptEngine.template("test","<% var a = function(){};\n" +
                    "  a(;) %>\n" +
                    "test",null);
        }catch (SyntaxException e){
            trace(e);
            assertTrue(e.getMessage().contains("a(;)"));
            b = true;
        }
        assertTrue(b);

        //clear caches
        scriptEngine.invalidateCache("test");
        scriptEngine.template("test","test",null);
    }

    @Test
    public void testEvalInFunction(){
        int p=10;

        final S1ScriptEngine scriptEngine = new S1ScriptEngine();
        assertEquals(p, LoadTestUtils.run("test",p,p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception {
                assertEquals(10,scriptEngine.evalInFunction(Integer.class,"return 10;",null).intValue());
                assertTrue(scriptEngine.evalInFunction(Boolean.class,"return true;",null));
                assertFalse(scriptEngine.evalInFunction(Boolean.class,"return false;",null));

            }
        }));
    }

    @Test
    public void testTimeLimit(){
        int p=1;

        final S1ScriptEngine scriptEngine = new S1ScriptEngine();
        scriptEngine.setTimeLimit(2000);
        final AtomicLong al = new AtomicLong();
        final Map<String,Object> data = Objects.newHashMap(
                "sleep",
                new ScriptFunction(new Context(1000), Objects.newArrayList(String.class,"time")) {
                    @Override
                    public Object call() throws ScriptException {
                        sleep(getContext().get(Long.class, "time"));
                        return null;
                    }
                },
                "tick",
                new ScriptFunction(new Context(1000), Objects.newArrayList(String.class)) {
                    @Override
                    public Object call() throws ScriptException {
                        al.incrementAndGet();
                        return null;
                    }
                }
        );
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {
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
                    scriptEngine.eval("while(true){tick();sleep(100);}", data);
                } catch (ScriptLimitException e){
                    if(input==0)
                        trace(e.getType()+":"+e.getMessage());
                    b = e.getType()== ScriptLimitException.Limits.TIME;
                }
                assertTrue(b);
                long l = al.get();
                sleep(1000);
                assertEquals(l,al.get());

            }
        }));
    }

    @Test
    public void testSizeLimit(){
        int p=10;

        final S1ScriptEngine scriptEngine = new S1ScriptEngine();
        scriptEngine.setSizeLimit(10);

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception {
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

            }
        }));
    }

    @Test
    public void testMemoryLimit(){
        int t = Thread.activeCount();
        int p=10;

        final S1ScriptEngine scriptEngine = new S1ScriptEngine();
        scriptEngine.setMemoryLimit(1000);

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception {
                String t = scriptEngine.eval("'012345'", null);
                assertEquals("012345", t);

                scriptEngine.eval("var a = {b:{c:{d:['1234567890']}}};for(var i=0;i<1000;i++){a.b.c.d[0]='test';}", null);

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

            }
        }));
        int tc = Options.getStorage().getSystem(Integer.class,S1ScriptEngine.OPTIONS_KEY+".threadCount",500);
        //4*p threads are requested
        trace(Thread.activeCount());
        assertTrue(t<=Thread.activeCount()+tc);
    }

    @Test
    public void testSyntaxError(){

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

    @Test
    public void testOptionsFunctions(){

        final S1ScriptEngine scriptEngine = new S1ScriptEngine();
        assertTrue(Objects.equals(5,scriptEngine.eval("test.sum(2,3);",null)));

    }

}