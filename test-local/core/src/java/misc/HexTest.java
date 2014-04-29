package misc;

import org.s1.misc.Hex;
import org.s1.testing.BasicTest;
import org.s1.testing.LoadTestUtils;
import org.testng.annotations.Test;


/**
 * s1v2
 * User: GPykhov
 * Date: 17.01.14
 * Time: 15:11
 */
public class HexTest extends BasicTest {

    @Test
    public void test1(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {
                String s = "qwer";
                String b = "71776572";

                assertEquals(b, Hex.encode(s.getBytes()));
                assertEquals(s, new String(Hex.decode(b)));

            }
        }));
    }
}
