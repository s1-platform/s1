package local.background;


import local.ClusterTest;
import org.s1.background.BackgroundWorker;
import org.s1.testing.BasicTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 10:45
 */
public class BackgroundListenerTest extends ClusterTest {

    @BeforeClass
    public void before(){
        BackgroundWorker.startAll();
    }

    @AfterClass
    public void after(){
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
