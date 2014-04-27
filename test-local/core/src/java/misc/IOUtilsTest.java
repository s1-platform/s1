package misc;

import org.s1.misc.IOUtils;
import org.s1.testing.BasicTest;
import org.s1.testing.LoadTestUtils;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * s1v2
 * User: GPykhov
 * Date: 17.01.14
 * Time: 15:11
 */
public class IOUtilsTest extends BasicTest {

    @Test
    public void testToString(){
        int p = 10;
        title("IOUtils.toString, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {
                String s = "qwerty12345asdf";
                InputStream is = new ByteArrayInputStream(s.getBytes(Charset.forName("UTF-8")));

                assertEquals(s, IOUtils.toString(is, "UTF-8"));
            }
        }));
    }

    @Test
    public void testCopy(){
        int p = 10;
        title("IOUtils.copy, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {
                String s = "qwerty12345asdf";
                InputStream is = new ByteArrayInputStream(s.getBytes(Charset.forName("UTF-8")));
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                long l = 0;
                try {
                    l = IOUtils.copy(is, os);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                assertEquals(s.length(), (int) l);
                assertEquals(s, new String(os.toByteArray(), Charset.forName("UTF-8")));

            }
        }));
    }

    @Test
    public void testCopySkip(){
        int p = 10;
        title("IOUtils.copy, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {
                String s = "qwerty12345asdf";
                InputStream is = new ByteArrayInputStream(s.getBytes(Charset.forName("UTF-8")));
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                long l = 0;
                try {
                    l = IOUtils.copy(is, os, 7, 4);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                assertEquals(4, (int) l);
                assertEquals("2345", new String(os.toByteArray(), Charset.forName("UTF-8")));

            }
        }));
    }
}
