package misc;

import org.s1.S1SystemError;
import org.s1.misc.FileUtils;
import org.s1.misc.IOUtils;
import org.s1.testing.BasicTest;
import org.s1.testing.LoadTestUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 22:36
 */
public class FileUtilsTest extends BasicTest {

    @Test
    public void testString(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {
                String s = "qwerty12345asdf";
                try{
                    FileUtils.writeStringToFile(new File(System.getProperty("java.io.tmpdir")+File.separator+"f"+index),s,"UTF-8");
                    assertEquals(s, FileUtils.readFileToString(new File(System.getProperty("java.io.tmpdir")+File.separator+"f"+index), "UTF-8"));
                }catch (IOException e){
                    throw S1SystemError.wrap(e);
                }
            }
        }));
    }

    @Test
    public void testReadResource(){
        int p = 1;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {
                try {
                    String s = resourceAsString("/misc/test.txt");
                    String d = IOUtils.toString(FileUtils.readResource("classpath:/misc/test.txt"), "UTF-8");
                    String d1 = IOUtils.toString(FileUtils.readResource("classpath:misc/test.txt"), "UTF-8");
                    String d2 = IOUtils.toString(FileUtils.readResource("classpath://misc/test.txt"), "UTF-8");
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
                    d = IOUtils.toString(FileUtils.readResource("file:"+path), "UTF-8");
                    assertEquals(s, d);
                    //test http://
                    d = IOUtils.toString(FileUtils.readResource("http://example.com"), "UTF-8");
                    assertTrue(d.contains("Example Domain"));

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }));
    }
}
