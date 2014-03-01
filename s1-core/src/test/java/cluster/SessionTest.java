package cluster;

import org.s1.cluster.Session;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.options.Options;
import org.s1.test.BasicTest;
import org.s1.test.LoadTestUtils;

/**
 * s1v2
 * User: GPykhov
 * Date: 17.01.14
 * Time: 15:11
 */
public class SessionTest extends BasicTest {

    public void testSession(){
        int p = 1000;
        title("Session, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) throws ClosureException {

                Session.run("test" + index, new Closure<String, Object>() {
                    @Override
                    public Object call(String input) throws ClosureException {

                        Session.getSessionBean().set("a", null);
                        Session.getSessionBean().setUserId(Session.ANONYMOUS);

                        assertNull(Session.getSessionBean().get("a"));
                        assertEquals(Session.ANONYMOUS, Session.getSessionBean().getUserId());

                        Session.getSessionBean().set("a", "test_" + input);
                        Session.getSessionBean().setUserId("user_" + input);

                        assertEquals("test_" + input, Session.getSessionBean().get("a"));
                        assertEquals("user_" + input, Session.getSessionBean().getUserId());

                        return null;
                    }
                });
                //String p = Options.getStorage().getParameter("config");

                return null;
            }
        }));
    }

    public void testClear(){
        int p = 10;
        title("Clear, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) throws ClosureException {

                Session.run("test" + index, new Closure<String, Object>() {
                    @Override
                    public Object call(String input) throws ClosureException {

                        Session.getSessionBean().set("a", null);
                        Session.getSessionBean().setUserId(Session.ANONYMOUS);

                        assertNull(Session.getSessionBean().get("a"));
                        assertEquals(Session.ANONYMOUS, Session.getSessionBean().getUserId());

                        Session.getSessionBean().set("a", "test_" + input);
                        Session.getSessionBean().setUserId("user_" + input);

                        assertEquals("test_" + input, Session.getSessionBean().get("a"));
                        assertEquals("user_" + input, Session.getSessionBean().getUserId());
                        return null;
                    }
                });
                //String p = Options.getStorage().getParameter("config");

                return null;
            }
        }));

        //read second time ok
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) throws ClosureException {

                Session.run("test" + index, new Closure<String, Object>() {
                    @Override
                    public Object call(String input) throws ClosureException {

                        assertEquals("test_" + input, Session.getSessionBean().get("a"));
                        assertEquals("user_" + input, Session.getSessionBean().getUserId());
                        return null;
                    }
                });
                //String p = Options.getStorage().getParameter("config");

                return null;
            }
        }));

        try {
            Thread.sleep(Options.getStorage().getSystem(Long.class,"session.ttl")+2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) throws ClosureException {

                Session.run("test" + index, new Closure<String, Object>() {
                    @Override
                    public Object call(String input) {

                        assertNull(Session.getSessionBean().get("a"));
                        assertEquals(Session.ANONYMOUS, Session.getSessionBean().getUserId());

                        return null;
                    }
                });
                //String p = Options.getStorage().getParameter("config");

                return null;
            }
        }));
    }

}
