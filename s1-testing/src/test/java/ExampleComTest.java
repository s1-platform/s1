import org.s1.testing.BasicTest;
import org.s1.testing.HttpServerTest;
import org.s1.testing.httpclient.TestHttpClient;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.nio.charset.Charset;

/**
 * @author Grigory Pykhov
 */
public class ExampleComTest extends HttpServerTest{

    @Override
    protected String getHost() {
        return "example.com";
    }

    @Override
    protected int getPort() {
        return 80;
    }

    @Test()
    public void test1(){
        title("example.com");
        TestHttpClient.HttpResponseBean b = client().get("/", null, null);
        Assert.assertEquals(b.getStatus(),200);
        trace(b.getHeaders());
        trace(new String(b.getData(), Charset.forName("UTF-8")));
        Assert.assertTrue(new String(b.getData(), Charset.forName("UTF-8")).contains("Example Domain"));
    }

}
