package cluster;

import org.s1.cluster.Locks;
import org.s1.cluster.LongRunningTasks;
import org.s1.testing.BasicTest;
import org.s1.testing.LoadTestUtils;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author Grigory Pykhov
 */
public class LongRunningTaskTest extends BasicTest {

    @Test
    public void testLock(){
        int p = 10;

        final String id = LongRunningTasks.start();

        for(int i=0;i<p;i++){
            LongRunningTasks.setProgress(id,i);
        }
        assertEquals((long)p-1, LongRunningTasks.getProgress(id));
        LongRunningTasks.finish(id);
        assertEquals(-1L, LongRunningTasks.getProgress(id));
    }

}
