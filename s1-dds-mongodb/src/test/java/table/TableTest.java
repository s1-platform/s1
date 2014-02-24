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

import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 21.02.14
 * Time: 10:57
 */
public class TableTest extends ClusterTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MongoDBConnectionHelper.getConnection(null)
                .getCollection(Options.getStorage().get(String.class, "table/test1", "collection")).remove(new BasicDBObject());
        trace("Cleared");
    }

    public void testFactory() {
        int p = 100;
        title("Factory, parallel: " + p);
        TablesFactory.getTable("test1");
        boolean b = false;
        try {
            TablesFactory.getTable("test_not_found");
        } catch (Throwable e) {
            b = true;
        }
        assertTrue(b);

        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                Table t = TablesFactory.getTable("test1");
                assertEquals("test1", t.getName());
                assertEquals("coll1", t.getCollection());
                assertNotNull(t.getAccess());
                assertNotNull(t.getEnrichRecord());
                assertNotNull(t.getPrepareSearch());
                assertNotNull(t.getPrepareSort());
                assertNotNull(t.getImportAccess());
                assertNotNull(t.getLogAccess());
                assertNotNull(t.getImportAction());
                assertNotNull(t.getSchema());
                assertEquals(3, t.getActions().size());
                assertEquals(1, t.getStates().size());
                assertEquals(2, t.getIndexes().size());
                return null;
            }
        }));
    }

    public void testUnique() {
        final int p = 100;
        title("Unique, parallel: " + p);
        TablesFactory.getTable("test1");

        //add, set
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(final Integer input) throws ClosureException {
                Session.run("s_" + input, new Closure<String, Object>() {
                    @Override
                    public Object call(String s) throws ClosureException {

                        try {
                            String id = null;
                            Map<String, Object> m = null;

                            boolean b = false;
                            Table t = TablesFactory.getTable("test1");
                            try{
                                //add
                                m = t.changeState(null, "add", Objects.newHashMap(String.class, Object.class,
                                        "a", "a",
                                        "b", 1
                                ), null);
                            }catch (AlreadyExistsException e){

                            }

                        } catch (Throwable e) {
                            throw ClosureException.wrap(e);
                        }

                        return null;
                    }
                });
                return null;
            }
        }));

        final List<Map<String,Object>> l = Objects.newArrayList();
        try{
            assertEquals(1L,TablesFactory.getTable("test1").list(l, null, null, null, null, 0, 10));
            assertEquals("a",Objects.get(l.get(0),"a"));
            assertEquals(1,Objects.get(l.get(0),"b"));
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

    public void testComplex() {
        final int p = 10;
        title("Complex, parallel: " + p);
        TablesFactory.getTable("test1");

        final Map<Integer, String> ids = Objects.newHashMap();

        //add, set
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(final Integer input) throws ClosureException {
                Session.run("s_" + input, new Closure<String, Object>() {
                    @Override
                    public Object call(String s) throws ClosureException {

                        try {
                            String id = null;
                            Map<String, Object> m = null;

                            boolean b = false;
                            Table t = TablesFactory.getTable("test1");
                            assertEquals(1, t.getAvailableActions(null).size());

                            //add
                            m = t.changeState(null, "add", Objects.newHashMap(String.class, Object.class,
                                    "a", "a_" + input,
                                    "b", -1
                            ), null);
                            id = Objects.get(m, "id");
                            ids.put(input, id);
                            assertNotNull(id);
                            assertEquals("present", Objects.get(m, Table.STATE));
                            assertEquals("a_" + input, Objects.get(m, "a"));
                            assertEquals(-1, Objects.get(m, "b"));

                            //set
                            assertEquals(2, t.getAvailableActions(id).size());
                            m = t.changeState(id, "setB", Objects.newHashMap(String.class, Object.class,
                                    "b1", input
                            ), Objects.newHashMap(String.class, Object.class,
                                    "comment", "comment " + input
                            ));
                            assertEquals("present", Objects.get(m, Table.STATE));
                            assertEquals(id, Objects.get(m, "id"));
                            assertEquals("a_" + input, Objects.get(m, "a"));
                            assertEquals(input, Objects.get(m, "b"));

                            //get
                            m = t.get(id);
                            assertEquals("present", Objects.get(m, Table.STATE));
                            assertEquals(id, Objects.get(m, "id"));
                            assertEquals("a_" + input, Objects.get(m, "a"));
                            assertEquals(input, Objects.get(m, "b"));

                        } catch (Throwable e) {
                            throw ClosureException.wrap(e);
                        }

                        return null;
                    }
                });
                return null;
            }
        }));

        //list, log
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(final Integer input) throws ClosureException {
                Session.run("s_" + input, new Closure<String, Object>() {
                    @Override
                    public Object call(String s) throws ClosureException {
                        try {
                            String id = ids.get(input);
                            Map<String, Object> m = null;
                            boolean b = false;
                            long c = 0;
                            Table t = TablesFactory.getTable("test1");

                            //list
                            List<Map<String, Object>> l = Objects.newArrayList();
                            c = t.list(l, null, new Query(new FieldQueryNode(
                                    "a", FieldQueryNode.FieldOperation.EQUALS, "a_"+input
                            )), null, null, 0, 10);
                            assertEquals(1L, c);
                            assertEquals(1, l.size());
                            assertEquals(id, Objects.get(l.get(0), "id"));

                            //list all
                            l.clear();
                            c = t.list(l, null, null, new Sort("a",false), null, 0, 10);
                            assertEquals((long) p, c);
                            assertEquals(Math.min(10,p), l.size());
                            assertEquals("a_0", Objects.get(l.get(0), "a"));

                            //list full-text
                            l.clear();
                            c = t.list(l, "a_0", null, null, null, 0, 10);
                            assertEquals(1L, c);
                            assertEquals(1, l.size());
                            assertEquals("a_0", Objects.get(l.get(0), "a"));

                            //aggregate
                            AggregationBean ab = t.aggregate("b", null);
                            assertEquals(0,ab.getMin());
                            assertEquals(p-1,ab.getMax());
                            double avg = 0;
                            for(int i=0;i<p;i++){
                                avg+=i;
                            }
                            avg = avg/p;
                            assertEquals(avg,ab.getAvg());
                            assertTrue((Integer) ab.getSum() >= p);
                            assertEquals(p,ab.getCount());

                            //count group
                            List<CountGroupBean> lc = t.countGroup("b", null);
                            if(input==0)
                                trace(lc);
                            lc = t.countGroup("a",null);
                            if(input==0)
                                trace(lc);

                            //log
                            l.clear();
                            c = t.listLog(l, id, null, 0, 10);
                            assertEquals(1L, c);
                            assertEquals(1, l.size());
                            assertEquals(id, Objects.get(l.get(0), "record"));
                            assertEquals("comment " + input, Objects.get(l.get(0), "foundation.comment"));
                            assertEquals("present", Objects.get(l.get(0), "action.from"));
                            assertEquals("present", Objects.get(l.get(0), "action.to"));
                            assertEquals("setB", Objects.get(l.get(0), "action.name"));
                            assertEquals("Set b", Objects.get(l.get(0), "action.label"));
                            assertEquals(input, Objects.get(l.get(0), "data.b1"));
                            assertEquals(input, Objects.get(l.get(0), "new.b"));
                            assertEquals(-1, Objects.get(l.get(0), "old.b"));
                            assertEquals(input, Objects.get(l.get(0), "changes[0].new"));
                            assertEquals(-1, Objects.get(l.get(0), "changes[0].old"));
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
                });
                return null;
            }
        }));

        //remove, log, trash
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(final Integer input) throws ClosureException {
                Session.run("s_" + input, new Closure<String, Object>() {
                    @Override
                    public Object call(String s) throws ClosureException {
                        try {
                            String id = ids.get(input);
                            Map<String, Object> m = null;
                            boolean b = false;
                            Table t = TablesFactory.getTable("test1");

                            //remove
                            assertEquals(2, t.getAvailableActions(id).size());
                            m = t.changeState(id, "remove", null, Objects.newHashMap(String.class, Object.class,
                                    "number", "num_" + input));
                            assertEquals(id, Objects.get(m, "id"));
                            assertEquals("a_" + input, Objects.get(m, "a"));
                            assertEquals(input, Objects.get(m, "b"));

                            try {
                                t.get(id);
                            } catch (NotFoundException e) {
                                b = true;
                            }
                            assertTrue(b);

                        } catch (Throwable e) {
                            throw ClosureException.wrap(e);
                        }
                        return null;
                    }
                });
                return null;
            }
        }));
    }

    public void testExpImport() {
        final int p = 1;
        final int c = 10;
        title("Export/import, parallel: " + p);
        TablesFactory.getTable("test1");

        //import new
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(final Integer input) throws ClosureException {
                Session.run("s_" + input, new Closure<String, Object>() {
                    @Override
                    public Object call(String s) throws ClosureException {

                        try {
                            List<Map<String,Object>> l = Objects.newArrayList();
                            for(int i=0;i<c;i++){
                                l.add(Objects.newHashMap(String.class,Object.class,
                                        "a","test_"+i,
                                        "b",i
                                        ));
                            }
                            l.add(Objects.newHashMap(String.class,Object.class,
                                    "a1","test_"
                            ));
                            l = TablesFactory.getTable("test1").doImport(l);
                            assertEquals(c+1,l.size());
                        } catch (Throwable e) {
                            throw ClosureException.wrap(e);
                        }

                        return null;
                    }
                });
                return null;
            }
        }));

        final List<Map<String,Object>> l = Objects.newArrayList();
        try{
            assertEquals((long)c,TablesFactory.getTable("test1").list(l, null, null, new Sort("b", false), null, 0, 10));
            assertEquals("test_0",Objects.get(l.get(0),"a"));
            assertEquals(0,Objects.get(l.get(0),"b"));
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }

        //import - update existing
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(final Integer input) throws ClosureException {
                Session.run("s_" + input, new Closure<String, Object>() {
                    @Override
                    public Object call(String s) throws ClosureException {

                        try {
                            Objects.set(l.get(0),"a","qwer_0");
                            TablesFactory.getTable("test1").doImport(l);
                        } catch (Throwable e) {
                            throw ClosureException.wrap(e);
                        }

                        return null;
                    }
                });
                return null;
            }
        }));

        try{
            assertEquals((long)c,TablesFactory.getTable("test1").list(l, null, null, new Sort("b", false), null, 0, 10));
            assertEquals("qwer_0",Objects.get(l.get(0),"a"));
            assertEquals(0,Objects.get(l.get(0),"b"));
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

}
