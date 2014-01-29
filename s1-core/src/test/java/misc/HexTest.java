package misc;

import org.s1.misc.Closure;
import org.s1.misc.Hex;
import org.s1.test.BasicTest;
import org.s1.test.LoadTestUtils;

/**
 * s1v2
 * User: GPykhov
 * Date: 17.01.14
 * Time: 15:11
 */
public class HexTest extends BasicTest {

    public void test1(){
        int p = 1000;
        title("Hex encode/decode, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) {
                String s = "qwer";
                String b = "71776572";

                assertEquals(b, Hex.encode(s.getBytes()));
                assertEquals(s, new String(Hex.decode(b)));

                return null;
            }
        }));
    }
}
