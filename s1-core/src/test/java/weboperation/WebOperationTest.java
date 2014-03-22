package weboperation;

import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;
import org.s1.test.LoadTestUtils;
import org.s1.test.ServerTest;

import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 10:45
 */
public class WebOperationTest extends ServerTest {

    public void testGetEcho(){
        int p = 10;
        title("Echo test, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                Map<String,Object> r = client().getJSON(getContext() + "/dispatcher/Echo",
                        Objects.newHashMap(String.class, Object.class, "aa", "qwer"), null);
                assertEquals("qwer",Objects.get(r,"aa"));
                assertEquals("aaa",Objects.get(r,"a"));
                try{
                    client().getJSON(getContext() + "/dispatcher/Echo.error",
                            Objects.newHashMap(String.class, Object.class), null);
                }catch (RuntimeException e){
                    trace(e.getMessage());
                    assertTrue(e.getMessage().contains("test error"));
                }
                return null;
            }
        }));
    }

    public void testPostEcho(){
        int p = 10;
        title("Echo test post, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                Map<String,Object> r = client().postJSON(getContext() + "/dispatcher/Echo",
                        Objects.newHashMap(String.class, Object.class, "aa", "qwer"), null);
                assertEquals("qwer",Objects.get(r,"aa"));
                assertEquals("aaa",Objects.get(r,"a"));
                try{
                    client().postJSON(getContext() + "/dispatcher/Echo.error",
                            Objects.newHashMap(String.class, Object.class), null);
                }catch (RuntimeException e){
                    trace(e.getMessage());
                    assertTrue(e.getMessage().contains("test error"));
                }
                return null;
            }
        }));
    }

    public void testProcessClassMethods(){
        int p = 10;
        title("Process class methods, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                Map<String,Object> r = client().getJSON(getContext() + "/dispatcher/Operation2.method1",
                        null, null);
                assertEquals("1",Objects.get(r,"a"));

                r = client().getJSON(getContext() + "/dispatcher/Operation2.method2",
                        null, null);
                assertEquals("2",Objects.get(r,"a"));

                r = client().getJSON(getContext() + "/dispatcher/Operation2.method3",
                        null, null);
                assertEquals("3",Objects.get(r,"a"));


                return null;
            }
        }));
    }

}
