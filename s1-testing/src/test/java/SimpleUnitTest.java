import org.s1.testing.BasicTest;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Grigory Pykhov
 */
public class SimpleUnitTest extends BasicTest{


    @Test()
    public void test1(){
        title("test1");
        //trace(getProjectHome());
        Assert.assertEquals("a","a");
    }

}
