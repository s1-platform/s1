package cluster.datasource;

import org.s1.cluster.datasource.NumberSequence;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.options.Options;
import org.s1.test.ClusterTest;
import org.s1.test.LoadTestUtils;

import java.io.File;

/**
 * s1v2
 * User: GPykhov
 * Date: 17.01.14
 * Time: 15:11
 */
public class NumberSequenceTest extends ClusterTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        File dir = new File(Options.getStorage().getSystem("numberSequence.home", System.getProperty("user.home") + File.separator + ".s1-sequences"));
        trace("NumberSequence.Home: " + dir.getAbsolutePath());
        boolean b = true;
        if(dir.exists()){
            for(File f:dir.listFiles()){
                b = b&&f.delete();
            }
        }
        trace("Clear: " + b);
    }

    public void testSequence(){
        int p = 100;
        title("Number sequence, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) throws ClosureException {

                for (long i = 0; i < 10L; i++) {
                    assertEquals(i, NumberSequence.next("qwe" + index));
                }

                return null;
            }
        }));
    }

}
