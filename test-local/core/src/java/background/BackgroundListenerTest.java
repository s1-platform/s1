package background;

import org.s1.background.BackgroundWorker;
import org.s1.cluster.ClusterLifecycleAction;
import org.s1.testing.ClusterTest;
import org.testng.annotations.*;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 10:45
 */
public class BackgroundListenerTest extends ClusterTest {

    @BeforeClass
    public void startBG(){
        BackgroundWorker.startAll();
    }

    @AfterClass
    public void stopBG(){
        BackgroundWorker.stopAll();
    }

    @Test
    public void testBackground(){
        title("Background");
        sleep(21000);
        assertTrue(TestBGWorker.a1 >= 10);
        assertTrue(TestBGWorker.a2 >= 2);
    }

}
