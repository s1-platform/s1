package cluster;

import org.s1.cluster.Locks;
import org.s1.cluster.LongRunningTasks;
import org.s1.testing.BasicTest;
import org.s1.testing.LoadTestUtils;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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

        LoadTestUtils.run("test",p,p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int i) throws Exception {
                LongRunningTasks.addProgress(id,2);
            }
        });
        assertEquals((long)p-1+p*2, LongRunningTasks.getProgress(id));
        LongRunningTasks.finish(id);
        assertEquals(-1L, LongRunningTasks.getProgress(id));
    }

}
