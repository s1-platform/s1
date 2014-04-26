package local.misc;

import org.s1.S1SystemError;
import org.s1.misc.Base64;
import org.s1.misc.Base64FormatException;
import org.s1.misc.Closure;
import org.s1.testing.BasicTest;
import org.s1.testing.LoadTestUtils;
import org.testng.annotations.Test;


import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * s1v2
 * User: GPykhov
 * Date: 17.01.14
 * Time: 15:11
 */
public class Base64Test extends BasicTest {

    @Test
    public void test1(){
        int p = 10;
        title("Base64 encode/decode, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {

                String s = "qwer";
                String b = "cXdlcg==";

                assertEquals(b, Base64.encode(s.getBytes()));
                try {
                    assertEquals(s, new String(Base64.decode(b)));
                } catch (Base64FormatException e) {
                    throw S1SystemError.wrap(e);
                }
            }
        }));
    }

    @Test
    public void test2(){
        int p = 1;
        title("Base64 decode, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {
                String b = resourceAsString("/misc/base64.txt");

                try {
                    byte e [] = Base64.decode(b);
                    if(index==0)
                        trace(e.length);
                } catch (Exception e) {
                    throw S1SystemError.wrap(e);
                }

            }
        }));
    }

}
