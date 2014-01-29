package misc;

import org.s1.misc.Closure;
import org.s1.misc.IOUtils;
import org.s1.test.BasicTest;
import org.s1.test.LoadTestUtils;

import java.io.*;
import java.nio.charset.Charset;

/**
 * s1v2
 * User: GPykhov
 * Date: 17.01.14
 * Time: 15:11
 */
public class IOUtilsTest extends BasicTest {

    public void testToString(){
        int p = 1000;
        title("IOUtils.toString, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) {
                String s = "qwerty12345asdf";
                InputStream is = new ByteArrayInputStream(s.getBytes(Charset.forName("UTF-8")));

                assertEquals(s, IOUtils.toString(is, "UTF-8"));

                return null;
            }
        }));
    }

    public void testCopy(){
        int p = 1000;
        title("IOUtils.copy, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) {
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

                return null;
            }
        }));
    }

    public void testCopySkip(){
        int p = 1000;
        title("IOUtils.copy, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) {
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

                return null;
            }
        }));
    }
}
