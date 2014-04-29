package cluster;

import org.s1.cluster.Session;
import org.s1.options.Options;
import org.s1.testing.BasicTest;
import org.s1.testing.LoadTestUtils;
import org.testng.annotations.Test;


/**
 * s1v2
 * User: GPykhov
 * Date: 17.01.14
 * Time: 15:11
 */
public class SessionTest extends BasicTest {

    @Test
    public void testSession(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {

                try {
                    Session.start("test" + index);

                    Session.getSessionBean().set("a", null);
                    Session.getSessionBean().setUserId(Session.ANONYMOUS);

                    assertNull(Session.getSessionBean().get("a"));
                    assertEquals(Session.ANONYMOUS, Session.getSessionBean().getUserId());

                    Session.getSessionBean().set("a", "test_" + index);
                    Session.getSessionBean().setUserId("user_" + index);

                    assertEquals("test_" + index, Session.getSessionBean().get("a"));
                    assertEquals("user_" + index, Session.getSessionBean().getUserId());

                }finally {
                    Session.end("test" + index);
                }
            }
        }));
    }

    @Test
    public void testClear(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {

                try {
                    Session.start("test" + index);

                    Session.getSessionBean().set("a", null);
                    Session.getSessionBean().setUserId(Session.ANONYMOUS);

                    assertNull(Session.getSessionBean().get("a"));
                    assertEquals(Session.ANONYMOUS, Session.getSessionBean().getUserId());

                    Session.getSessionBean().set("a", "test_" + index);
                    Session.getSessionBean().setUserId("user_" + index);

                    assertEquals("test_" + index, Session.getSessionBean().get("a"));
                    assertEquals("user_" + index, Session.getSessionBean().getUserId());

                }finally {
                    Session.end("test" + index);
                }

            }
        }));

        //read second time ok
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {

                try {
                    Session.start("test" + index);

                    assertEquals("test_" + index, Session.getSessionBean().get("a"));
                    assertEquals("user_" + index, Session.getSessionBean().getUserId());

                }finally {
                    Session.end("test" + index);
                }
            }
        }));

        try {
            Thread.sleep(Options.getStorage().getSystem(Long.class,"session.ttl")+2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {

                try {
                    Session.start("test" + index);

                    assertNull(Session.getSessionBean().get("a"));
                    assertEquals(Session.ANONYMOUS, Session.getSessionBean().getUserId());

                }finally {
                    Session.end("test" + index);
                }

            }
        }));
    }

}
