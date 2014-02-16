package cluster.monitor;

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
public class MonitorWebTest extends ServerTest {

    public void testLog(){
        int p = 1;
        title("Log, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                TestHttpClient client = client();
                boolean b = true;
                try{
                    client.postJSON(getContext()+"/dispatcher/Monitor.clusterInfo",Objects.newHashMap(
                            String.class,Object.class
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

                //cluster info
                Map<String,Object> m = client.postJSON(getContext()+"/dispatcher/Monitor.clusterInfo",Objects.newHashMap(
                        String.class,Object.class,
                        "nodeId","node-1"
                ),null);


                //node indicators
                m = client.postJSON(getContext()+"/dispatcher/Monitor.nodeIndicators",Objects.newHashMap(
                        String.class,Object.class,
                        "nodeId","node-1"
                ),null);

                //root logger
                client.postJSON(getContext()+"/dispatcher/Monitor.setLogLevel",Objects.newHashMap(
                        String.class,Object.class,
                        "nodeId","node-1",
                        "level","ERROR"
                ),null);

                //org.s1.web.DispatcherServlet
                client.postJSON(getContext()+"/dispatcher/Monitor.setLogLevel",Objects.newHashMap(
                        String.class,Object.class,
                        "nodeId","node-1",
                        "level","TRACE","name","org.s1.web.DispatcherServlet"
                ),null);

                //get
                m = client.postJSON(getContext()+"/dispatcher/Monitor.getLoggers",Objects.newHashMap(
                        String.class,Object.class,
                        "nodeId","node-1"
                ),null);

                assertEquals("ERROR",Objects.get(m,"level"));
                assertEquals("TRACE",Objects.get(Map.class,m,"loggers").get("org.s1.web.DispatcherServlet"));

                return null;
            }
        }));
    }

}
