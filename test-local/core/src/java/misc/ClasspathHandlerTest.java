package misc;

import org.s1.misc.IOUtils;
import org.s1.misc.protocols.Init;
import org.s1.testing.BasicTest;
import org.s1.testing.LoadTestUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;

/**
 * s1v2
 * User: GPykhov
 * Date: 17.01.14
 * Time: 15:11
 */
public class ClasspathHandlerTest extends BasicTest {

    static {
        Init.init();
        System.out.println("Init done!");
    }

    @Test
    public void test1(){
        int p = 1;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {
                try {
                    String s = resourceAsString("/misc/test.txt");
                    URLConnection c = new URL("classpath:/misc/test.txt").openConnection();
                    String d = IOUtils.toString(c.getInputStream(), "UTF-8");
                    URLConnection c1 = new URL("classpath:misc/test.txt").openConnection();
                    String d1 = IOUtils.toString(c1.getInputStream(), "UTF-8");
                    URLConnection c2 = new URL("classpath://misc/test.txt").openConnection();
                    String d2 = IOUtils.toString(c2.getInputStream(), "UTF-8");
                    if (index == 0) {
                        trace(s);
                        trace(d);
                        trace(d1);
                        trace(d2);
                    }
                    assertEquals(s, d);
                    assertEquals(s, d1);
                    assertEquals(s, d2);

                    //test file://
                    String path = new File(this.getClass().getResource("/misc/test.txt").toURI()).getAbsolutePath();
                    //file
                    c = new URL("file:" + path).openConnection();
                    d = IOUtils.toString(c.getInputStream(), "UTF-8");
                    assertEquals(s, d);
                    //test http://
                    c = new URL("http://example.com").openConnection();
                    d = IOUtils.toString(c.getInputStream(), "UTF-8");
                    assertTrue(d.contains("Example Domain"));

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }));
    }
}
