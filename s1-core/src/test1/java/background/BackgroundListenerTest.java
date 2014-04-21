package background;

import org.s1.test.ServerTest;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 10:45
 */
public class BackgroundListenerTest extends ServerTest {

    public void testBackground(){
        title("Background");
        sleep(21000);
        assertTrue(TestBGWorker.a1>=10);
        assertTrue(TestBGWorker.a2 >= 2);
    }

}
