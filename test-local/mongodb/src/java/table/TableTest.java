/*
 * Copyright 2014 Grigory Pykhov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package table;

import com.mongodb.BasicDBObject;
import org.s1.S1SystemError;
import org.s1.cluster.Session;
import org.s1.misc.Closure;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.objects.Objects;
import org.s1.script.Context;
import org.s1.script.S1ScriptEngine;
import org.s1.script.errors.ScriptException;
import org.s1.script.function.ScriptFunction;
import org.s1.table.AggregationBean;
import org.s1.table.ImportResultBean;
import org.s1.table.Table;
import org.s1.table.errors.AlreadyExistsException;
import org.s1.table.errors.NotFoundException;
import org.s1.table.format.FieldQueryNode;
import org.s1.table.format.Query;
import org.s1.table.format.Sort;
import org.s1.testing.ClusterTest;
import org.s1.testing.LoadTestUtils;
import org.s1.testing.HttpServerTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 21.02.14
 * Time: 10:57
 */
public class TableTest extends ClusterTest {

    @BeforeMethod
    protected void clear() throws Exception {
        Table t = new TestTable1();
        MongoDBConnectionHelper.getConnection(t.getCollectionId().getDatabase())
                .getCollection(t.getCollectionId().getCollection()).remove(new BasicDBObject());
        trace("Cleared");
    }

    @Test
    public void testUnique() {
        final int p = 10;

        final Table t = new TestTable1();
        //add, set
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input)  throws Exception {
                try {
                    Session.start("s_" + input);

                    String id = null;
                    Map<String, Object> m = null;

                    boolean b = false;

                    try {
                        //add
                        m = t.add("add", Objects.newHashMap(String.class, Object.class,
                                "a", "a",
                                "b", 1
                        ));
                    } catch (AlreadyExistsException e) {

                    } catch (Exception e) {
                        throw S1SystemError.wrap(e);
                    }
                } finally {
                    Session.end("s_" + input);
                }
            }
        }));

        final List<Map<String,Object>> l = Objects.newArrayList();
        try{
            assertEquals(1L,t.list(l, null, null, null, 0, 10));
            assertEquals("a",Objects.get(l.get(0),"a"));
            assertEquals(1,Objects.get(l.get(0),"b"));
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

    @Test
    public void testComplex() {
        final int p = 10;
        final Table t = new TestTable1();

        final Map<Integer, String> ids = Objects.newHashMap();

        //add, set
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input)  throws Exception {
                try {
                    Session.start("s_" + input);

                        try {
                            String id = null;
                            Map<String, Object> m = null;

                            boolean b = false;

                            //add
                            m = t.add("add", Objects.newHashMap(String.class, Object.class,
                                    "a", "a_" + input,
                                    "b", -1
                            ));
                            id = Objects.get(m, "id");
                            ids.put(input, id);
                            assertNotNull(id);
                            assertEquals("a_" + input, Objects.get(m, "a"));
                            assertEquals(-1, Objects.get(m, "b"));

                            //set
                            m = t.set(id, "setB", Objects.newHashMap(String.class, Object.class,
                                    "b1", input
                            ));
                            assertEquals(id, Objects.get(m, "id"));
                            assertEquals("a_" + input, Objects.get(m, "a"));
                            assertEquals(input, Objects.get(m, "b"));

                            //get
                            m = t.get(id);
                            assertEquals(id, Objects.get(m, "id"));
                            assertEquals("a_" + input, Objects.get(m, "a"));
                            assertEquals(input, Objects.get(m, "b"));

                        } catch (Throwable e) {
                            throw S1SystemError.wrap(e);
                        }

                } finally {
                    Session.end("s_" + input);
                }
            }
        }));

        //list, log
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input)  throws Exception {
                try {
                    Session.start("s_" + input);

                        try {
                            String id = ids.get(input);
                            Map<String, Object> m = null;
                            boolean b = false;
                            long c = 0;

                            //list
                            List<Map<String, Object>> l = Objects.newArrayList();
                            c = t.list(l, new Query(new FieldQueryNode(
                                    "a", FieldQueryNode.FieldOperation.EQUALS, "a_"+input
                            )), null, null, 0, 10);
                            assertEquals(1L, c);
                            assertEquals(1, l.size());
                            assertEquals(id, Objects.get(l.get(0), "id"));

                            //list all
                            l.clear();
                            c = t.list(l, null, new Sort("a",false), null, 0, 10);
                            assertEquals((long) p, c);
                            assertEquals(Math.min(10,p), l.size());
                            assertEquals("a_0", Objects.get(l.get(0), "a"));

                            //list full-text
                            /*l.clear();
                            c = t.list(l, new Query(null,Objects.newHashMap(String.class,Object.class,
                                    "$text","a_0")), null, null, 0, 10);
                            assertEquals(1L, c);
                            assertEquals(1, l.size());
                            assertEquals("a_0", Objects.get(l.get(0), "a"));*/

                            //aggregate



                        } catch (Throwable e) {
                            throw S1SystemError.wrap(e);
                        }
                } finally {
                    Session.end("s_" + input);
                }
            }
        }));

        //remove,
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input)  throws Exception {
                try {
                Session.start("s_" + input);

                        try {
                            String id = ids.get(input);
                            Map<String, Object> m = null;
                            boolean b = false;

                            //remove
                            m = t.remove(id, "remove", null);
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
                            throw S1SystemError.wrap(e);
                        }
                } finally {
                    Session.end("s_" + input);
                }
            }
        }));
    }

    @Test
    public void testExpImport() {
        final int p = 10;
        final int c = 10;
        final Table t = new TestTable1();

        //import new
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input)  throws Exception {
                try {
                Session.start("s_" + input);

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
                            List<ImportResultBean> lr = t.doImport(l);
                            assertEquals(c+1,lr.size());
                        } catch (Throwable e) {
                            throw S1SystemError.wrap(e);
                        }

                } finally {
                    Session.end("s_" + input);
                }
            }
        }));

        final List<Map<String,Object>> l = Objects.newArrayList();
        try{
            assertEquals((long)c,t.list(l, null, new Sort("b", false), null, 0, 10));
            assertEquals("test_0",Objects.get(l.get(0),"a"));
            assertEquals(0,Objects.get(l.get(0),"b"));
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }

        //import - update existing
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input)  throws Exception {
                try {
                Session.start("s_" + input);

                        try {
                            Objects.set(l.get(0),"a","qwer_0");
                            t.doImport(l);
                        } catch (Throwable e) {
                            throw S1SystemError.wrap(e);
                        }

                } finally {
                    Session.end("s_" + input);
                }
            }
        }));

        try{
            assertEquals((long)c,t.list(l, null, new Sort("b", false), null, 0, 10));
            assertEquals("qwer_0",Objects.get(l.get(0),"a"));
            assertEquals(0,Objects.get(l.get(0),"b"));
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

    @Test
    public void testScript() {
        final int p = 10;
        final int c = 10;
        final Table t = new TestTable1();

        try {
            Session.start("s_" + 1);

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
                List<ImportResultBean> lr = t.doImport(l);
                assertEquals(c+1,l.size());
            } catch (Throwable e) {
                throw S1SystemError.wrap(e);
            }

        } finally {
            Session.end("s_" + 1);
        }

        S1ScriptEngine se = new S1ScriptEngine();

        String t1 = Objects.cast(se.eval(null,"var count = 0;\n" +
                "var list = [];\n" +
                "count = table.list('table1',list,{},{},{},0,10,{});\n" +
                "s1.length(list);",Objects.newSOHashMap()),String.class);
        assertEquals(""+c,t1);
        String t2 = se.template(null,"<%var count = 0;\n" +
                "var list = [];\n" +
                "count = table.list('table1',list,{},{},{},0,10,{});\n" +
                "%><%=s1.length(list)%>",Objects.newSOHashMap());
        assertEquals(""+c,t2);
    }

}
