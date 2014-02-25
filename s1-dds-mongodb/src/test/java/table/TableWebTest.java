package table;

import com.mongodb.BasicDBObject;
import org.s1.S1SystemError;
import org.s1.cluster.Session;
import org.s1.cluster.datasource.AlreadyExistsException;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.s1.table.AggregationBean;
import org.s1.table.CountGroupBean;
import org.s1.table.Table;
import org.s1.table.TablesFactory;
import org.s1.table.format.FieldQueryNode;
import org.s1.table.format.Query;
import org.s1.table.format.Sort;
import org.s1.test.ClusterTest;
import org.s1.test.LoadTestUtils;
import org.s1.test.ServerTest;

import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 21.02.14
 * Time: 10:57
 */
public class TableWebTest extends ServerTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MongoDBConnectionHelper.getConnection(null)
                .getCollection(Options.getStorage().get(String.class, "table/test1", "collection")).remove(new BasicDBObject());
        trace("Cleared");
    }

    public void testComplex() {
        final int p = 1;
        title("Complex, parallel: " + p);

        final Map<Integer, String> ids = Objects.newHashMap();

        //add, set
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(final Integer input) throws ClosureException {
                        try {
                            String id = null;
                            Map<String, Object> m = null;

                            boolean b = false;
                            assertEquals(1,Objects.get(List.class,client().postJSON(getContext()+"/dispatcher/Table.getAvailableActions",Objects.newHashMap(String.class,Object.class,
                                    "table","test1",
                                    "id",id),null),"list").size());

                            //add
                            m = client().postJSON(getContext()+"/dispatcher/Table.changeState",Objects.newHashMap(String.class,Object.class,
                                    "table","test1",
                                    "action","add",
                                    "data",Objects.newHashMap("a","a_"+input,"b",-1)),null);

                            id = Objects.get(m, "id");
                            ids.put(input, id);
                            assertNotNull(id);
                            assertEquals("present", Objects.get(m, Table.STATE));
                            assertEquals("a_" + input, Objects.get(m, "a"));
                            assertEquals(-1, Objects.get(Integer.class, m, "b").intValue());

                            //set
                            assertEquals(2,Objects.get(List.class,client().postJSON(getContext()+"/dispatcher/Table.getAvailableActions",Objects.newHashMap(String.class,Object.class,
                                    "table","test1",
                                    "id",id),null),"list").size());

                            m = client().postJSON(getContext()+"/dispatcher/Table.changeState",Objects.newHashMap(String.class,Object.class,
                                    "table","test1",
                                    "action","setB",
                                    "id",id,
                                    "data",Objects.newHashMap("b1",input),
                                    "foundation",Objects.newHashMap("comment","comment "+input)),null);

                            assertEquals("present", Objects.get(m, Table.STATE));
                            assertEquals(id, Objects.get(m, "id"));
                            assertEquals("a_" + input, Objects.get(m, "a"));
                            assertEquals(input, Objects.get(Integer.class,m, "b"));

                            //get
                            m = client().postJSON(getContext()+"/dispatcher/Table.get",Objects.newHashMap(String.class,Object.class,
                                    "table","test1",
                                    "id",id),null);

                            assertEquals("present", Objects.get(m, Table.STATE));
                            assertEquals(id, Objects.get(m, "id"));
                            assertEquals("a_" + input, Objects.get(m, "a"));
                            assertEquals(input, Objects.get(Integer.class,m, "b"));

                        } catch (Throwable e) {
                            throw ClosureException.wrap(e);
                        }

                return null;
            }
        }));

        //list, log
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(final Integer input) throws ClosureException {
                        try {
                            String id = ids.get(input);
                            Map<String, Object> m = null;
                            boolean b = false;
                            long c = 0;
                            //list
                            List<Map<String, Object>> l = Objects.newArrayList();

                            m = client().postJSON(getContext()+"/dispatcher/Table.list",Objects.newHashMap(String.class,Object.class,
                                    "table","test1",
                                    "skip",0,"max",10,
                                    "search",Objects.newHashMap("node",Objects.newHashMap("field","a","operation","equals","value","a_"+input))
                                    ),null);
                            l = Objects.get(m,"list");
                            c = Objects.get(m,"count");

                            assertEquals(1L, c);
                            assertEquals(1, l.size());
                            assertEquals(id, Objects.get(l.get(0), "id"));

                            //list all
                            l.clear();
                            m = client().postJSON(getContext()+"/dispatcher/Table.list",Objects.newHashMap(String.class,Object.class,
                                    "table","test1",
                                    "skip",0,"max",10,
                                    "sort",Objects.newHashMap("name","a","desc",false)
                            ),null);
                            l = Objects.get(m,"list");
                            c = Objects.get(m,"count");
                            assertEquals((long) p, c);
                            assertEquals(Math.min(10,p), l.size());
                            assertEquals("a_0", Objects.get(l.get(0), "a"));

                            //list full-text
                            l.clear();
                            m = client().postJSON(getContext()+"/dispatcher/Table.list",Objects.newHashMap(String.class,Object.class,
                                    "table","test1",
                                    "skip",0,"max",10,
                                    "text","a_0"
                            ),null);
                            l = Objects.get(m,"list");
                            c = Objects.get(m,"count");
                            assertEquals(1L, c);
                            assertEquals(1, l.size());
                            assertEquals("a_0", Objects.get(l.get(0), "a"));

                            //aggregate
                            m = client().postJSON(getContext()+"/dispatcher/Table.aggregate",Objects.newHashMap(String.class,Object.class,
                                    "table","test1",
                                    "field","b"
                            ),null);
                            assertEquals(0,Objects.get(Integer.class,m,"min").intValue());
                            assertEquals(p-1,Objects.get(Integer.class,m,"max").intValue());
                            double avg = 0;
                            for(int i=0;i<p;i++){
                                avg+=i;
                            }
                            avg = avg/p;
                            assertEquals(avg,Objects.get(Double.class,m,"avg").doubleValue());
                            assertTrue(Objects.get(Long.class,m,"sum").longValue() >= p-1);
                            assertEquals(p,Objects.get(Integer.class,m,"count").intValue());

                            //count group
                            m = client().postJSON(getContext()+"/dispatcher/Table.countGroup",Objects.newHashMap(String.class,Object.class,
                                    "table","test1",
                                    "field","b"
                            ),null);
                            assertTrue(Objects.get(List.class,m,"list").size()>0);
                            if(input==0)
                                trace(Objects.get(m,"list"));
                            m = client().postJSON(getContext()+"/dispatcher/Table.countGroup",Objects.newHashMap(String.class,Object.class,
                                    "table","test1",
                                    "field","a"
                            ),null);
                            assertTrue(Objects.get(List.class,m,"list").size()>0);
                            if(input==0)
                                trace(Objects.get(m,"list"));

                            //log
                            l.clear();
                            m = client().postJSON(getContext()+"/dispatcher/Table.listLog",Objects.newHashMap(String.class,Object.class,
                                    "table","test1",
                                    "skip",0,"max",10,
                                    "id",id
                            ),null);
                            l = Objects.get(m,"list");
                            c = Objects.get(m,"count");
                            assertEquals(1L, c);
                            assertEquals(1, l.size());
                            assertEquals(id, Objects.get(l.get(0), "record"));
                            assertEquals("comment " + input, Objects.get(l.get(0), "foundation.comment"));
                            assertEquals("present", Objects.get(l.get(0), "action.from"));
                            assertEquals("present", Objects.get(l.get(0), "action.to"));
                            assertEquals("setB", Objects.get(l.get(0), "action.name"));
                            assertEquals("Set b", Objects.get(l.get(0), "action.label"));
                            assertEquals(input, Objects.get(Integer.class,l.get(0), "data.b1"));
                            assertEquals(input, Objects.get(Integer.class,l.get(0), "new.b"));
                            assertEquals(-1, Objects.get(Integer.class,l.get(0), "old.b").intValue());
                            assertEquals(input, Objects.get(Integer.class,l.get(0), "changes[0].new"));
                            assertEquals(-1, Objects.get(Integer.class,l.get(0), "changes[0].old").intValue());
                            assertEquals("b", Objects.get(l.get(0), "changes[0].path"));
                            assertEquals("bbb", Objects.get(l.get(0), "changes[0].label"));
                            assertNotNull(Objects.get(l.get(0), "date"));
                            assertEquals(Session.ANONYMOUS, Objects.get(l.get(0), "user"));
                            if (input == 0)
                                trace(l.get(0));

                        } catch (Throwable e) {
                            throw ClosureException.wrap(e);
                        }
                return null;
            }
        }));

        //remove, log, trash
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(final Integer input) throws ClosureException {
                        try {
                            String id = ids.get(input);
                            Map<String, Object> m = null;
                            boolean b = false;

                            //remove
                            assertEquals(2,Objects.get(List.class,client().postJSON(getContext()+"/dispatcher/Table.getAvailableActions",Objects.newHashMap(String.class,Object.class,
                                    "table","test1",
                                    "id",id),null),"list").size());

                            m = client().postJSON(getContext()+"/dispatcher/Table.changeState",Objects.newHashMap(String.class,Object.class,
                                    "table","test1",
                                    "action","remove",
                                    "id",id,
                                    "foundation",Objects.newHashMap("number","num_"+input)),null);

                            assertEquals(id, Objects.get(m, "id"));
                            assertEquals("a_" + input, Objects.get(m, "a"));
                            assertEquals(input, Objects.get(Integer.class,m, "b"));

                            try{
                                m = client().postJSON(getContext() + "/dispatcher/Table.get", Objects.newHashMap(String.class, Object.class,
                                        "table","test1",
                                        "id", id
                                ), null);
                            }catch (RuntimeException e){
                                b = true;
                            }
                            assertTrue(b);

                        } catch (Throwable e) {
                            throw ClosureException.wrap(e);
                        }

                return null;
            }
        }));
    }

}
