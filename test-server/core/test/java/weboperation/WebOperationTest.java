package weboperation;

import org.s1.misc.Closure;

import org.s1.objects.Objects;
import org.s1.testing.HttpServerTest;
import org.s1.testing.LoadTestUtils;
import org.testng.annotations.Test;


import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 10:45
 */
public class WebOperationTest extends HttpServerTest {

    @Test
    public void testGetEcho(){
        int p = 10;
        title("Echo test, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {
                Map<String,Object> r = client().getJSON(getContext() + "/dispatcher/Echo",
                        Objects.newHashMap(String.class, Object.class, "aa", "qwer"));
                assertEquals("qwer",Objects.get(r,"aa"));
                assertEquals("aaa",Objects.get(r,"a"));
                try{
                    client().getJSON(getContext() + "/dispatcher/Echo.error",
                            Objects.newHashMap(String.class, Object.class));
                }catch (RuntimeException e){
                    trace(e.getMessage());
                    assertTrue(e.getMessage().contains("test error"));
                }
            }
        }));
    }

    @Test
    public void testPostEcho(){
        int p = 10;
        title("Echo test post, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {
                Map<String,Object> r = client().postJSON(getContext() + "/dispatcher/Echo",
                        Objects.newHashMap(String.class, Object.class, "aa", "qwer"));
                assertEquals("qwer",Objects.get(r,"aa"));
                assertEquals("aaa",Objects.get(r,"a"));
                try{
                    client().postJSON(getContext() + "/dispatcher/Echo.error",
                            Objects.newHashMap(String.class, Object.class));
                }catch (RuntimeException e){
                    trace(e.getMessage());
                    assertTrue(e.getMessage().contains("test error"));
                }
            }
        }));
    }

    @Test
    public void testProcessClassMethods(){
        int p = 10;
        title("Process class methods, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {
                Map<String,Object> r = client().getJSON(getContext() + "/dispatcher/Operation2.method1",
                        null);
                assertEquals("1",Objects.get(r,"a"));

                r = client().getJSON(getContext() + "/dispatcher/Operation2.method2",
                        null);
                assertEquals("2",Objects.get(r,"a"));

                r = client().getJSON(getContext() + "/dispatcher/Operation2.method3",
                        null);
                assertEquals("3",Objects.get(r,"a"));

            }
        }));
    }

}
