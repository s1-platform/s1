package cluster;

import org.s1.cluster.Locks;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.test.BasicTest;
import org.s1.test.LoadTestUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * s1v2
 * User: GPykhov
 * Date: 17.01.14
 * Time: 15:11
 */
public class LockTest extends BasicTest {

    public void testLock(){
        int p = 10;
        title("Lock, parallel "+p);
        final StringBuffer buf = new StringBuffer();
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) throws ClosureException {

                try {
                    Locks.waitAndRun("test", new Closure<String, Object>() {
                        @Override
                        public Object call(String input) {
                            if (buf.length() == 0)
                                buf.append(".");
                            return null;
                        }
                    }, 30, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    throw new RuntimeException(e);
                }

                return null;
            }
        }));
        assertEquals(1, buf.length());
    }

    public void testHasLock(){
        int p = 10;
        title("Has lock, parallel "+p);
        final StringBuffer buf = new StringBuffer();
        final StringBuffer buf2 = new StringBuffer();
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) throws ClosureException {

                Locks.runIfFree("test", new Closure<String, Object>() {
                    @Override
                    public Object call(String input) {
                        buf2.append(".");
                        if (buf.length() == 0)
                            buf.append(".");
                        return null;
                    }
                });

                return null;
            }
        }));
        trace("were able to run: "+buf2.length());
        assertEquals(1, buf.length());
        assertTrue(buf2.length()>=1);
        assertTrue(buf2.length()<p);
    }

}
