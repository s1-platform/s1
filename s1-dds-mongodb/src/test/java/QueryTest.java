import com.mongodb.DB;
import com.mongodb.DBCollection;
import org.s1.S1SystemError;
import org.s1.cluster.datasource.MoreThanOneFoundException;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.mongodb.MongoDBFormat;
import org.s1.mongodb.MongoDBQueryHelper;
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
 * Time: 13:21
 */
public class QueryTest extends BasicTest {

    private static final String COLL = "coll_query";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        DB db = MongoDBConnectionHelper.getConnection(null);
        DBCollection coll = db.getCollection(COLL);
        coll.drop();
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
                    "bool", i % 3 == 0
            )));
        }
        trace("data inserted");
    }

    public void testGet(){
        int p=100;
        title("Get, parallel:"+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {

                Map<String,Object> m = null;
                try {
                    m = MongoDBQueryHelper.get(null, COLL, Objects.newHashMap(String.class, Object.class,
                            "id", 10));
                } catch (NotFoundException e) {
                    e.printStackTrace();
                } catch (MoreThanOneFoundException e) {
                    e.printStackTrace();
                }
                assertEquals(10,m.get("id"));
                assertEquals("test_10",m.get("str"));

                //not found
                boolean b = false;
                try {
                    MongoDBQueryHelper.get(null, COLL, Objects.newHashMap(String.class, Object.class,
                            "id", -10));
                } catch (NotFoundException e) {
                    b = true;
                } catch (MoreThanOneFoundException e) {
                    throw S1SystemError.wrap(e);
                }
                assertTrue(b);

                //>1
                b = false;
                try {
                    MongoDBQueryHelper.get(null, COLL, Objects.newHashMap(String.class, Object.class));
                } catch (NotFoundException e) {
                    throw S1SystemError.wrap(e);
                } catch (MoreThanOneFoundException e) {
                    b = true;
                }
                assertTrue(b);

                return null;
            }
        }));
    }

    public void testList(){
        int p=100;
        title("List, parallel:"+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                List<Map<String,Object>> res = Objects.newArrayList();
                long c = MongoDBQueryHelper.list(res,null,COLL,Objects.newHashMap(String.class,Object.class,
                        "id",Objects.newHashMap("$lte",50)),
                        Objects.newHashMap(String.class,Object.class,"id",-1),
                        Objects.newHashMap(String.class,Object.class,"str2",0),0,10);
                assertEquals(51L,c);
                assertEquals(10,res.size());
                assertEquals(50,res.get(0).get("id"));
                return null;
            }
        }));
    }

}
