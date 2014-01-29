package log;

import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;
import org.s1.test.LoadTestUtils;
import org.s1.test.ServerTest;
import org.s1.test.TestHttpClient;

import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 10:45
 */
public class LogWebTest extends ServerTest {

    public void testLog(){
        int p = 100;
        title("Log, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                TestHttpClient client = client();
                boolean b = true;
                try{
                    client.postJSON(getContext()+"/dispatcher/Log.set",Objects.newHashMap(
                            String.class,Object.class,
                            "level","ERROR"
                    ),null);
                    b = false;
                }catch (RuntimeException e){
                    if(input==0)
                        trace(e.getMessage());
                }
                assertTrue(b);

                //authenticate
                client.postJSON(getContext()+"/dispatcher/Auth.login",Objects.newHashMap(
                        String.class,Object.class,
                        "name","root",
                        "password","root"
                ),null);

                //root logger
                client.postJSON(getContext()+"/dispatcher/Log.set",Objects.newHashMap(
                        String.class,Object.class,
                        "level","ERROR"
                ),null);

                //org.s1.web.DispatcherServlet
                client.postJSON(getContext()+"/dispatcher/Log.set",Objects.newHashMap(
                        String.class,Object.class,
                        "level","TRACE","name","org.s1.web.DispatcherServlet"
                ),null);

                //get
                Map<String,Object> m = client.postJSON(getContext()+"/dispatcher/Log.get",Objects.newHashMap(
                        String.class,Object.class
                ),null);

                assertEquals("ERROR",Objects.get(m,"level"));
                assertEquals("TRACE",Objects.get(Map.class,m,"loggers").get("org.s1.web.DispatcherServlet"));

                return null;
            }
        }));
    }

}
