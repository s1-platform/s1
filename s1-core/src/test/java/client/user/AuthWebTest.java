package client.user;

import org.s1.misc.Closure;

import org.s1.objects.Objects;
import org.s1.testing.HttpServerTest;
import org.s1.testing.LoadTestUtils;
import org.s1.testing.httpclient.TestHttpClient;
import org.testng.annotations.Test;


import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 10:45
 */
public class AuthWebTest extends HttpServerTest {

    @Test
    public void testLoginLogout(){
        int p = 10;
        title("Root login, logout, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {

                TestHttpClient client = client();
                Map<String,Object> m = null;

                m = client.getJSON(getContext() + "/dispatcher/User.whoAmI", null);
                assertEquals("anonymous",Objects.get(m,"id"));

                m = client.getJSON(getContext() + "/dispatcher/User.getUser?id=root", null);
                assertEquals("root",Objects.get(m,"id"));

                boolean b=true;
                try{
                    client.postJSON(getContext() + "/dispatcher/User.login", Objects.newHashMap(
                            String.class, Object.class,
                            "name", "root",
                            "password", "wrong"
                    ));
                    b = false;
                }catch (RuntimeException e){
                    if(input==0)
                        trace(e.getMessage());
                    assertTrue(e.getMessage().contains("AuthException"));
                }
                assertTrue(b);

                //authenticate
                client.postJSON(getContext() + "/dispatcher/User.login", Objects.newHashMap(
                        String.class, Object.class,
                        "name", "root",
                        "password", "root"
                ));

                m = client.getJSON(getContext() + "/dispatcher/User.whoAmI", null);
                assertEquals("root",Objects.get(m,"id"));

                m = client.getJSON(getContext() + "/dispatcher/User.logout", null);

                m = client.getJSON(getContext() + "/dispatcher/User.whoAmI", null);
                assertEquals("anonymous",Objects.get(m,"id"));

            }
        }));
    }

}
