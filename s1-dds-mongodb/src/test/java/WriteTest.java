import com.mongodb.BasicDBObject;
import org.s1.S1SystemError;
import org.s1.cluster.Locks;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.mongodb.MongoDBDDS;
import org.s1.mongodb.MongoDBQueryHelper;
import org.s1.objects.Objects;
import org.s1.test.ClusterTest;
import org.s1.test.LoadTestUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.02.14
 * Time: 13:21
 */
public class WriteTest extends ClusterTest {

    private static final String COLL = "coll1";

    public void testAll(){
        int p=100;
        title("Add set remove, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                MongoDBDDS.add(null,COLL, Objects.newHashMap(String.class,Object.class,
                            "id",input
                        ),Objects.newHashMap(String.class,Object.class,
                            "id",input,"title","test_"+input
                        ));

                //
                Map<String,Object> m = null;
                try{
                    m = MongoDBQueryHelper.get(null,COLL,new BasicDBObject(
                            "id",input
                    ));
                }catch (Exception e){
                    throw S1SystemError.wrap(e);
                }
                assertEquals(input,m.get("id"));
                assertEquals("test_"+input,m.get("title"));
                m.put("x","y");

                //set
                MongoDBDDS.set(null, COLL, Objects.newHashMap(String.class, Object.class,
                        "id", input
                ), m);

                try{
                    m = MongoDBQueryHelper.get(null,COLL,new BasicDBObject(
                            "id",input
                    ));
                }catch (Exception e){
                    throw S1SystemError.wrap(e);
                }

                assertEquals(input,m.get("id"));
                assertEquals("test_"+input,m.get("title"));
                assertEquals("y",m.get("x"));

                //remove
                MongoDBDDS.remove(null, COLL, Objects.newHashMap(String.class, Object.class,
                        "id", input
                ));

                boolean b = false;
                try{
                    MongoDBQueryHelper.get(null,COLL,new BasicDBObject(
                            "id",input
                    ));
                }catch (NotFoundException e){
                    b = true;
                }catch (Exception e){
                    throw S1SystemError.wrap(e);
                }

                assertTrue(b);

                return null;
            }
        }));
    }

    public void testFlush(){
        int p=100;
        title("Flush, parallel: "+p);
        MongoDBDDS.add(null,COLL, Objects.newHashMap(String.class,Object.class,
                "id","test"
        ),Objects.newHashMap(String.class,Object.class,
                "id","test","count",0
        ));

        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                try{
                    Locks.waitAndRun("test",new Closure<String, Object>() {
                        @Override
                        public Object call(String input) throws ClosureException {
                            Map<String,Object> s = Objects.newHashMap(
                                    "id", "test"
                            );
                            MongoDBDDS.waitForRecord(null,COLL,s);

                            Map<String,Object> m = null;
                            try{
                                m = MongoDBQueryHelper.get(null,COLL,new BasicDBObject(s));
                            }catch (Exception e){
                                throw S1SystemError.wrap(e);
                            }
                            int c = Objects.get(m,"count");

                            m.put("count",c+1);

                            //save
                            MongoDBDDS.set(null,COLL,s,m);

                            return null;
                        }
                    },30, TimeUnit.SECONDS);
                }catch (TimeoutException e){
                    throw S1SystemError.wrap(e);
                }
                return null;
            }
        }));


        Map<String,Object> m = null;
        try{
            m = MongoDBQueryHelper.get(null,COLL,new BasicDBObject(
                    "id","test"
            ));
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
        assertEquals(p,m.get("count"));
    }


}
