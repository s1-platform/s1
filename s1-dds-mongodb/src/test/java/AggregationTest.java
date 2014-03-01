import com.mongodb.DB;
import com.mongodb.DBCollection;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.mongodb.MongoDBAggregationHelper;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.mongodb.MongoDBFormat;
import org.s1.objects.Objects;
import org.s1.table.AggregationBean;
import org.s1.table.CountGroupBean;
import org.s1.test.ClusterTest;
import org.s1.test.LoadTestUtils;

import java.util.Date;
import java.util.List;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.02.14
 * Time: 15:13
 */
public class AggregationTest extends ClusterTest{

    private static final String COLL = "coll_aggr";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        DB db = MongoDBConnectionHelper.getConnection(null);
        DBCollection coll = db.getCollection(COLL);
        coll.drop();
        long t = System.currentTimeMillis();
        for(int i=0;i<100;i++){
            coll.insert(MongoDBFormat.fromMap(Objects.newHashMap(String.class, Object.class,
                    "index", i,
                    "str", "test_" + i,
                    "str2", "test_" + (i % 50),
                    "int", i,
                    "float", i / 10.0F,
                    "double", i / 100.0D,
                    "long", 100000L * i,
                    "date", new Date(t + (i * 1000) % 120),
                    "date2", new Date(t + i),
                    "bool", i%3==0
            )));
        }
        trace("data inserted");
    }

    public void testAggregate(){
        int p=1;
        title("Aggregate, parallel:"+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                AggregationBean r = MongoDBAggregationHelper.aggregate(null,COLL,"str",null);
                if(input==0)
                    trace(r);
                assertEquals("test_0",r.getMin());
                assertEquals("test_99",r.getMax());
                assertEquals(100L,r.getCount());

                r = MongoDBAggregationHelper.aggregate(null,COLL,"str2",null);
                if(input==0)
                    trace(r);
                assertEquals("test_0",r.getMin());
                assertEquals("test_9",r.getMax());
                assertEquals(100L,r.getCount());
                assertEquals(0.0,r.getAvg());
                assertEquals(0,r.getSum());

                r = MongoDBAggregationHelper.aggregate(null,COLL,"int",null);
                if(input==0)
                    trace(r);
                assertEquals(0,r.getMin());
                assertEquals(99,r.getMax());
                assertEquals(100L,r.getCount());
                assertEquals(49.5,r.getAvg());
                assertEquals(4950,r.getSum());

                r = MongoDBAggregationHelper.aggregate(null,COLL,"float",null);
                if(input==0)
                    trace(r);
                assertEquals(0D,r.getMin());
                assertEquals(9.9D,r.getMax());
                assertEquals(100L,r.getCount());
                assertEquals(4.95D,r.getAvg());
                assertEquals(495D,r.getSum());

                r = MongoDBAggregationHelper.aggregate(null,COLL,"double",null);
                if(input==0)
                    trace(r);
                assertEquals(0D,r.getMin());
                assertEquals(0.99D,r.getMax());
                assertEquals(100L,r.getCount());
                assertEquals(0.495D,r.getAvg());
                assertEquals(49.5D,r.getSum());

                r = MongoDBAggregationHelper.aggregate(null,COLL,"long",null);
                if(input==0)
                    trace(r);
                assertEquals(0L,r.getMin());
                assertEquals(9900000L,r.getMax());
                assertEquals(100L,r.getCount());
                assertEquals(4950000.0,r.getAvg());
                assertEquals(495000000L,r.getSum());

                r = MongoDBAggregationHelper.aggregate(null,COLL,"bool",null);
                if(input==0)
                    trace(r);
                assertEquals(false,r.getMin());
                assertEquals(true,r.getMax());
                assertEquals(100L,r.getCount());

                r = MongoDBAggregationHelper.aggregate(null,COLL,"date",null);
                if(input==0)
                    trace(r);
                assertEquals(100L,r.getCount());

                r = MongoDBAggregationHelper.aggregate(null,COLL,"date2",null);
                if(input==0)
                    trace(r);
                assertEquals(100L,r.getCount());
                return null;
            }
        }));
    }

    public void testCountGroup(){
        int p=1;
        title("Count group, parallel:"+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {

                List<CountGroupBean> l = MongoDBAggregationHelper.countGroup(null,COLL,"str",null);
                if(input==0)
                    trace(l);
                assertEquals(21,l.size());
                assertEquals(1L,l.get(0).getCount());
                assertFalse(l.get(0).isOther());
                assertNotNull(l.get(0).getValue());
                assertEquals(80L,l.get(l.size() - 1).getCount());
                assertTrue(l.get(l.size() - 1).isOther());
                assertNull(l.get(l.size() - 1).getValue());

                l = MongoDBAggregationHelper.countGroup(null,COLL,"int",null);
                if(input==0)
                    trace(l);
                assertEquals(21,l.size());
                assertEquals(4L,l.get(0).getCount());

                l = MongoDBAggregationHelper.countGroup(null,COLL,"float",null);
                if(input==0)
                    trace(l);
                assertEquals(21,l.size());
                assertEquals(5L,l.get(0).getCount());

                l = MongoDBAggregationHelper.countGroup(null,COLL,"double",null);
                if(input==0)
                    trace(l);
                assertEquals(21,l.size());
                assertEquals(5L,l.get(0).getCount());

                l = MongoDBAggregationHelper.countGroup(null,COLL,"long",null);
                if(input==0)
                    trace(l);
                assertEquals(21,l.size());
                assertEquals(5L,l.get(0).getCount());

                l = MongoDBAggregationHelper.countGroup(null,COLL,"bool",null);
                if(input==0)
                    trace(l);
                assertEquals(2,l.size());

                l = MongoDBAggregationHelper.countGroup(null,COLL,"date",null);
                if(input==0)
                    trace(l);
                assertTrue(l.size() >= 1);

                l = MongoDBAggregationHelper.countGroup(null,COLL,"date2",null);
                if(input==0)
                    trace(l);
                assertTrue(l.size() >= 1);

                return null;
            }
        }));
    }

}