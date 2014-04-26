package local;

import org.s1.lifecycle.LifecycleAction;
import org.s1.testing.BasicTest;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;

/**
 * @author Grigory Pykhov
 */
public abstract class ClusterTest extends BasicTest{

    @BeforeTest
    public void beforeTest() {
        super.beforeTest();
        //start cluster
        LifecycleAction.startAll();
    }

    @AfterTest
    public void afterTest(){
        //stop cluster
        LifecycleAction.stopAll();
    }
}
