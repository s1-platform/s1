package misc;

import org.s1.S1SystemError;
import org.s1.misc.Closure;
import org.s1.misc.FileUtils;
import org.s1.test.BasicTest;
import org.s1.test.LoadTestUtils;

import java.io.File;
import java.io.IOException;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 22:36
 */
public class FileUtilsTest extends BasicTest {

    public void testString(){
        int p = 10;
        title("FileUtils string file, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) {
                String s = "qwerty12345asdf";
                try{
                    FileUtils.writeStringToFile(new File(System.getProperty("java.io.tmpdir")+File.separator+"f"+index),s,"UTF-8");
                    assertEquals(s, FileUtils.readFileToString(new File(System.getProperty("java.io.tmpdir")+File.separator+"f"+index), "UTF-8"));
                }catch (IOException e){
                    throw S1SystemError.wrap(e);
                }

                return null;
            }
        }));
    }
}
