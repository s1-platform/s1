import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.mongodb.MongoDBAggregationHelper;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.mongodb.MongoDBFormat;
import org.s1.objects.Objects;
import org.s1.test.BasicTest;
import org.s1.test.LoadTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.02.14
 * Time: 15:13
 */
public class AggregationTest extends BasicTest{

    private static final String COLL = "coll_aggr";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        DB db = MongoDBConnectionHelper.getConnection(null);
        DBCollection coll = db.getCollection(COLL);
        /*coll.drop();
        long t = System.currentTimeMillis();
        for(int i=0;i<100;i++){
            coll.insert(MongoDBFormat.fromMap(Objects.newHashMap(String.class, Object.class,
                    "id", i,
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
        } */
        trace("data inserted");
    }

    public void testAggregate(){
        int p=1;
        title("Aggregate, parallel:"+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                Map<String,Object> r = MongoDBAggregationHelper.aggregate(null,COLL,"str",null);
                if(input==0)
                    trace(r);
                assertEquals("test_0",r.get("min"));
                assertEquals("test_99",r.get("max"));
                assertEquals(100L,r.get("count"));

                r = MongoDBAggregationHelper.aggregate(null,COLL,"str2",null);
                if(input==0)
                    trace(r);
                assertEquals("test_0",r.get("min"));
                assertEquals("test_9",r.get("max"));
                assertEquals(100L,r.get("count"));
                assertEquals(0.0,r.get("avg"));
                assertEquals(0,r.get("sum"));

                r = MongoDBAggregationHelper.aggregate(null,COLL,"int",null);
                if(input==0)
                    trace(r);
                assertEquals(0,r.get("min"));
                assertEquals(99,r.get("max"));
                assertEquals(100L,r.get("count"));
                assertEquals(49.5,r.get("avg"));
                assertEquals(4950,r.get("sum"));

                r = MongoDBAggregationHelper.aggregate(null,COLL,"float",null);
                if(input==0)
                    trace(r);
                assertEquals(0D,r.get("min"));
                assertEquals(9.9D,r.get("max"));
                assertEquals(100L,r.get("count"));
                assertEquals(4.95D,r.get("avg"));
                assertEquals(495D,r.get("sum"));

                r = MongoDBAggregationHelper.aggregate(null,COLL,"double",null);
                if(input==0)
                    trace(r);
                assertEquals(0D,r.get("min"));
                assertEquals(0.99D,r.get("max"));
                assertEquals(100L,r.get("count"));
                assertEquals(0.495D,r.get("avg"));
                assertEquals(49.5D,r.get("sum"));

                r = MongoDBAggregationHelper.aggregate(null,COLL,"long",null);
                if(input==0)
                    trace(r);
                assertEquals(0L,r.get("min"));
                assertEquals(9900000L,r.get("max"));
                assertEquals(100L,r.get("count"));
                assertEquals(4950000.0,r.get("avg"));
                assertEquals(495000000L,r.get("sum"));

                r = MongoDBAggregationHelper.aggregate(null,COLL,"bool",null);
                if(input==0)
                    trace(r);
                assertEquals(false,r.get("min"));
                assertEquals(true,r.get("max"));
                assertEquals(100L,r.get("count"));

                r = MongoDBAggregationHelper.aggregate(null,COLL,"date",null);
                if(input==0)
                    trace(r);
                assertEquals(100L,r.get("count"));

                r = MongoDBAggregationHelper.aggregate(null,COLL,"date2",null);
                if(input==0)
                    trace(r);
                assertEquals(100L,r.get("count"));
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

                List<Map<String,Object>> l = MongoDBAggregationHelper.countGroup(null,COLL,"str",null);
                if(input==0)
                    trace(l);
                assertEquals(21,l.size());
                assertEquals(1L,l.get(0).get("count"));
                assertEquals(80L,l.get(l.size() - 1).get("other"));

                l = MongoDBAggregationHelper.countGroup(null,COLL,"int",null);
                if(input==0)
                    trace(l);
                assertEquals(21,l.size());
                assertEquals(4L,l.get(0).get("count"));

                l = MongoDBAggregationHelper.countGroup(null,COLL,"float",null);
                if(input==0)
                    trace(l);
                assertEquals(21,l.size());
                assertEquals(5L,l.get(0).get("count"));

                l = MongoDBAggregationHelper.countGroup(null,COLL,"double",null);
                if(input==0)
                    trace(l);
                assertEquals(21,l.size());
                assertEquals(5L,l.get(0).get("count"));

                l = MongoDBAggregationHelper.countGroup(null,COLL,"long",null);
                if(input==0)
                    trace(l);
                assertEquals(21,l.size());
                assertEquals(5L,l.get(0).get("count"));

                l = MongoDBAggregationHelper.countGroup(null,COLL,"bool",null);
                if(input==0)
                    trace(l);
                assertEquals(2,l.size());

                l = MongoDBAggregationHelper.countGroup(null,COLL,"date",null);
                if(input==0)
                    trace(l);
                assertEquals(3,l.size());

                l = MongoDBAggregationHelper.countGroup(null,COLL,"date2",null);
                if(input==0)
                    trace(l);
                assertEquals(2,l.size());

                return null;
            }
        }));
    }

}
