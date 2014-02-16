package cluster.monitor;

import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;
import org.s1.test.LoadTestUtils;
import org.s1.test.ServerTest;
import org.s1.test.TestHttpClient;
import org.s1.weboperation.WebOperation;

import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 10:45
 */
public class MonitorWebTest extends ServerTest {

    public void testMonitor(){
        int p = 10;
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
                Map<String,Object> m =null;
                //cluster info
                m = client.postJSON(getContext()+"/dispatcher/Monitor.clusterInfo",Objects.newHashMap(
                        String.class,Object.class,
                        "nodeId","node-1"
                ),null);
                assertEquals(1,Objects.get(List.class,m,"nodes").size());
                assertEquals("node-1", Objects.get(m, "nodes", Objects.newArrayList(Map.class)).get(0).get("nodeId"));

                //node indicators
                m = client.postJSON(getContext()+"/dispatcher/Monitor.nodeInfo",Objects.newHashMap(
                        String.class,Object.class,
                        "nodeId","node-1"
                ),null);
                assertEquals("node-1",Objects.get(m, "nodeId"));
                assertNotNull(Objects.get(m, "freeMemory"));

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
