package monitor;

import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.s1.testing.HttpServerTest;
import org.s1.testing.LoadTestUtils;
import org.s1.testing.httpclient.TestHttpClient;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 10:45
 */
public class MonitorWebTest extends HttpServerTest {

    @Test
    public void testMonitor(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test",p,p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {
                TestHttpClient client = client();

                Map<String,Object> m =null;
                //cluster info
                m = client.postJSON(getContext()+"/dispatcher/Monitor.getClusterInfo",Objects.newHashMap(
                        String.class,Object.class,
                        "nodeId","node-1"
                ));
                assertEquals(1,Objects.get(List.class,m,"nodes").size());
                assertEquals("node-1", Objects.get(m, "nodes", Objects.newArrayList(Map.class)).get(0).get("nodeId"));

                //node indicators
                m = client.postJSON(getContext()+"/dispatcher/Monitor.getNodeInfo",Objects.newHashMap(
                        String.class,Object.class,
                        "nodeId","node-1"
                ));
                assertEquals("node-1",Objects.get(m, "nodeId"));
                assertNotNull(Objects.get(m, "freeMemory"));

                //root logger
                client.postJSON(getContext()+"/dispatcher/Monitor.setLogLevel",Objects.newHashMap(
                        String.class,Object.class,
                        "nodeId","node-1",
                        "level","ERROR"
                ));

                //org.s1.web.DispatcherServlet
                client.postJSON(getContext()+"/dispatcher/Monitor.setLogLevel",Objects.newHashMap(
                        String.class,Object.class,
                        "nodeId","node-1",
                        "level","TRACE","name","org.s1.web.DispatcherServlet"
                ));

                //get
                m = client.postJSON(getContext()+"/dispatcher/Monitor.getLoggers",Objects.newHashMap(
                        String.class,Object.class,
                        "nodeId","node-1"
                ));

                assertEquals("ERROR",Objects.get(m,"level"));
                assertEquals("TRACE",Objects.get(Map.class,m,"loggers").get("org.s1.web.DispatcherServlet"));

            }
        }));
    }

}
