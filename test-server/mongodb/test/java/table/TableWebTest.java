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
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.objects.Objects;
import org.s1.table.Table;
import org.s1.testing.HttpServerTest;
import org.s1.testing.LoadTestUtils;
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
public class TableWebTest extends HttpServerTest {

    @BeforeMethod
    protected void clear() throws Exception {
        Table t = new TestTable1();
        MongoDBConnectionHelper.getConnection(t.getCollectionId().getDatabase())
                .getCollection(t.getCollectionId().getCollection()).remove(new BasicDBObject());
        trace("Cleared");
    }

    @Test
    public void testComplex() {
        final int p = 10;

        final Map<Integer, String> ids = Objects.newHashMap();

        //add, set
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input)  throws Exception {
                        try {
                            String id = null;
                            Map<String, Object> m = null;

                            boolean b = false;
                            //add
                            m = client().postJSON(getContext()+"/dispatcher/Table.add",Objects.newHashMap(String.class,Object.class,
                                    "table","table1",
                                    "action","add",
                                    "data",Objects.newHashMap("a","a_"+input,"b",-1)));

                            id = Objects.get(m, "id");
                            ids.put(input, id);
                            assertNotNull(id);
                            assertEquals("a_" + input, Objects.get(m, "a"));
                            assertEquals(-1, Objects.get(Integer.class, m, "b").intValue());

                            //set
                            m = client().postJSON(getContext()+"/dispatcher/Table.set",Objects.newHashMap(String.class,Object.class,
                                    "table","table1",
                                    "action","setB",
                                    "id",id,
                                    "data",Objects.newHashMap("b1",input)));

                            assertEquals(id, Objects.get(m, "id"));
                            assertEquals("a_" + input, Objects.get(m, "a"));
                            assertEquals(input, Objects.get(Integer.class,m, "b"));

                            //get
                            m = client().postJSON(getContext()+"/dispatcher/Table.get",Objects.newHashMap(String.class,Object.class,
                                    "table","table1",
                                    "id",id));

                            assertEquals(id, Objects.get(m, "id"));
                            assertEquals("a_" + input, Objects.get(m, "a"));
                            assertEquals(input, Objects.get(Integer.class,m, "b"));

                        } catch (Throwable e) {
                            throw S1SystemError.wrap(e);
                        }

            }
        }));

        //list, log
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input)  throws Exception {
                        try {
                            String id = ids.get(input);
                            Map<String, Object> m = null;
                            boolean b = false;
                            long c = 0;
                            //list
                            List<Map<String, Object>> l = Objects.newArrayList();

                            m = client().postJSON(getContext()+"/dispatcher/Table.list",Objects.newHashMap(String.class,Object.class,
                                    "table","table1",
                                    "skip",0,"max",10,
                                    "search",Objects.newHashMap("node",Objects.newHashMap("field","a","operation","equals","value","a_"+input))
                                    ));
                            l = Objects.get(m,"list");
                            c = Objects.get(m,"count");

                            assertEquals(1L, c);
                            assertEquals(1, l.size());
                            assertEquals(id, Objects.get(l.get(0), "id"));

                            //list all
                            l.clear();
                            m = client().postJSON(getContext()+"/dispatcher/Table.list",Objects.newHashMap(String.class,Object.class,
                                    "table","table1",
                                    "skip",0,"max",10,
                                    "sort",Objects.newHashMap("name","a","desc",false)
                            ));
                            l = Objects.get(m,"list");
                            c = Objects.get(m,"count");
                            assertEquals((long) p, c);
                            assertEquals(Math.min(10,p), l.size());
                            assertEquals("a_0", Objects.get(l.get(0), "a"));

                            //list full-text
                            /*l.clear();
                            m = client().postJSON(getContext()+"/dispatcher/Table.list",Objects.newHashMap(String.class,Object.class,
                                    "table","table1",
                                    "skip",0,"max",10,
                                    "search",Objects.newHashMap("custom",Objects.newHashMap("$text","a_0"))
                            ),null);
                            l = Objects.get(m,"list");
                            c = Objects.get(m,"count");
                            assertEquals(1L, c);
                            assertEquals(1, l.size());
                            assertEquals("a_0", Objects.get(l.get(0), "a"));*/




                        } catch (Throwable e) {
                            throw S1SystemError.wrap(e);
                        }
            }
        }));

        //remove
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input)  throws Exception {
                        try {
                            String id = ids.get(input);
                            Map<String, Object> m = null;
                            boolean b = false;

                            //remove

                            m = client().postJSON(getContext()+"/dispatcher/Table.remove",Objects.newHashMap(String.class,Object.class,
                                    "table","table1",
                                    "action","remove",
                                    "id",id));

                            assertEquals(id, Objects.get(m, "id"));
                            assertEquals("a_" + input, Objects.get(m, "a"));
                            assertEquals(input, Objects.get(Integer.class,m, "b"));

                            try{
                                m = client().postJSON(getContext() + "/dispatcher/Table.get", Objects.newHashMap(String.class, Object.class,
                                        "table","table1",
                                        "id", id
                                ));
                            }catch (RuntimeException e){
                                b = true;
                            }
                            assertTrue(b);

                        } catch (Throwable e) {
                            throw S1SystemError.wrap(e);
                        }
            }
        }));
    }

}
