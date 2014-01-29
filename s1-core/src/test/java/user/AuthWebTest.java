package user;

import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;
import org.s1.test.LoadTestUtils;
import org.s1.test.ServerTest;
import org.s1.test.TestHttpClient;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 10:45
 */
public class AuthWebTest extends ServerTest {

    public void testRoot(){
        int p = 100;
        title("Auth root, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {

                TestHttpClient client = client();
                boolean b=true;
                try{
                    client.postJSON(getContext() + "/dispatcher/Auth.login", Objects.newHashMap(
                            String.class, Object.class,
                            "name", "root",
                            "password", "wrong"
                    ), null);
                    b = false;
                }catch (RuntimeException e){
                    if(input==0)
                        trace(e.getMessage());
                    assertTrue(e.getMessage().contains("AuthException"));
                }
                assertTrue(b);

                //authenticate
                client.postJSON(getContext() + "/dispatcher/Auth.login", Objects.newHashMap(
                        String.class, Object.class,
                        "name", "root",
                        "password", "root"
                ), null);

                return null;
            }
        }));
    }

}
