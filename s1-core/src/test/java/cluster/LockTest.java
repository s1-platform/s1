package cluster;

import com.hazelcast.core.Hazelcast;
import org.s1.cluster.HazelcastWrapper;
import org.s1.cluster.Locks;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;
import org.s1.test.BasicTest;
import org.s1.test.LoadTestUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * s1v2
 * User: GPykhov
 * Date: 17.01.14
 * Time: 15:11
 */
public class LockTest extends BasicTest {

    public void testLock(){
        int p = 10;
        title("Lock, parallel "+p);
        final StringBuffer buf = new StringBuffer();
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) throws ClosureException {

                try {
                    Locks.waitAndRun("test", new Closure<String, Object>() {
                        @Override
                        public Object call(String input) {
                            if (buf.length() == 0)
                                buf.append(".");
                            return null;
                        }
                    }, 30, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    throw new RuntimeException(e);
                }

                return null;
            }
        }));
        assertEquals(1, buf.length());
    }

    public void testNestedLock(){
        int p = 10;
        title("Nested Lock, parallel "+p);
        final Map<String,Integer> m = Objects.newHashMap("a",0);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) throws ClosureException {

                try {
                    Locks.waitAndRun("test", new Closure<String, Object>() {
                        @Override
                        public Object call(String input) throws ClosureException{
                            try{

                                Locks.waitAndRun("test", new Closure<String, Object>() {
                                    @Override
                                    public Object call(String input) {
                                        int i = m.get("a");
                                        m.put("a",i+1);
                                        return null;
                                    }
                                }, 30, TimeUnit.SECONDS);

                                Locks.waitAndRun("test", new Closure<String, Object>() {
                                    @Override
                                    public Object call(String input) {
                                        int i = m.get("a");
                                        m.put("a",i+1);
                                        return null;
                                    }
                                }, 30, TimeUnit.SECONDS);

                                }catch (Exception e){
                                    throw ClosureException.wrap(e);
                                }
                            return null;
                        }
                    }, 30, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    throw new RuntimeException(e);
                }

                return null;
            }
        }));
        assertEquals(p*2, m.get("a").intValue());
    }

    public void testMultiLock(){
        HazelcastWrapper.getInstance();
        int p = 100;
        title("Multi Lock, parallel "+p);
        final ConcurrentHashMap<String,Object> cm = new ConcurrentHashMap<String, Object>();
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(final Integer index) throws ClosureException {
                try {
                    String lockId = "db/coll";
                    if(index%2==0)
                        lockId = "db/coll/id"+index;
                    final String lid = lockId;
                    Locks.waitAndRun(lockId, new Closure<String, Object>() {
                        @Override
                        public Object call(String input) {
                            //no lock with same name
                            assertFalse(cm.containsValue(lid));
                            sleep(10);
                            //put self
                            cm.put("" + index, lid);
                            sleep(10);
                            if(index%2==0){
                                assertTrue(cm.size() >= 1);
                            }else{
                                assertEquals(1,cm.size());
                            }
                            sleep(10);
                            cm.remove(""+index);
                            return null;
                        }
                    }, 30, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    throw new RuntimeException(e);
                }

                return null;
            }
        }));
        assertTrue(cm.size()==0);
    }

    public void testMultiLockGame(){
        HazelcastWrapper.getInstance();
        final int p = 1000;
        final int c = 20;
        final int sum = 100;
        final int pay = 2;
        title("Multi Lock game, parallel "+p);
        final Map<String,Integer> cm = Objects.newHashMap();
        for(int i=0;i<c;i++){
            cm.put("acc_"+i,sum);
        }
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(final Integer index) throws ClosureException {
                try {
                    final int acc1 = (int)Math.floor(Math.random()*10000000%c);
                    final int acc2 = (int)Math.floor(Math.random() * 10000000 % c);
                    if(acc1==acc2)
                        return null;

                    Locks.waitAndRun(Objects.newArrayList(
                            //"db/coll"
                            "db/coll/acc_"+acc1,
                            "db/coll/acc_"+acc2
                    ), new Closure<String, Object>() {
                        @Override
                        public Object call(String input) {
                            //no lock with same name
                            int bal1 = cm.get("acc_"+acc1);
                            int bal2 = cm.get("acc_"+acc2);
                            if(bal1-pay>=0){
                                bal1-=pay;
                                bal2+=pay;
                            }
                            sleep(10);
                            cm.put("acc_"+acc1,bal1);
                            cm.put("acc_"+acc2,bal2);

                            return null;
                        }
                    }, 30, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    throw new RuntimeException(e);
                }

                return null;
            }
        }));

        //
        trace(cm);
        int total = 0;
        for(int i:cm.values()){
            total+=i;
        }
        assertEquals(sum*c,total);
    }

}
