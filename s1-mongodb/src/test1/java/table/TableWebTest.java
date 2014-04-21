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
import org.s1.options.Options;
import org.s1.table.Table;
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
        Table t = new TestTable1();
        MongoDBConnectionHelper.getConnection(t.getCollectionId().getDatabase())
                .getCollection(t.getCollectionId().getCollection()).remove(new BasicDBObject());
        trace("Cleared");
    }

    public void testComplex() {
        final int p = 10;
        title("Complex, parallel: " + p);

        final Map<Integer, String> ids = Objects.newHashMap();

        //add, set
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(final Integer input)  {
                        try {
                            String id = null;
                            Map<String, Object> m = null;

                            boolean b = false;
                            //add
                            m = client().postJSON(getContext()+"/dispatcher/Table.add",Objects.newHashMap(String.class,Object.class,
                                    "table","table1",
                                    "action","add",
                                    "data",Objects.newHashMap("a","a_"+input,"b",-1)),null);

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
                                    "data",Objects.newHashMap("b1",input)),null);

                            assertEquals(id, Objects.get(m, "id"));
                            assertEquals("a_" + input, Objects.get(m, "a"));
                            assertEquals(input, Objects.get(Integer.class,m, "b"));

                            //get
                            m = client().postJSON(getContext()+"/dispatcher/Table.get",Objects.newHashMap(String.class,Object.class,
                                    "table","table1",
                                    "id",id),null);

                            assertEquals(id, Objects.get(m, "id"));
                            assertEquals("a_" + input, Objects.get(m, "a"));
                            assertEquals(input, Objects.get(Integer.class,m, "b"));

                        } catch (Throwable e) {
                            throw S1SystemError.wrap(e);
                        }

                return null;
            }
        }));

        //list, log
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(final Integer input)  {
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
                                    ),null);
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
                            ),null);
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

                            //aggregate
                            m = client().postJSON(getContext()+"/dispatcher/Table.aggregate",Objects.newHashMap(String.class,Object.class,
                                    "table","table1",
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
                                    "table","table1",
                                    "field","b"
                            ),null);
                            assertTrue(Objects.get(List.class,m,"list").size()>0);
                            if(input==0)
                                trace(Objects.get(m,"list"));
                            m = client().postJSON(getContext()+"/dispatcher/Table.countGroup",Objects.newHashMap(String.class,Object.class,
                                    "table","table1",
                                    "field","a"
                            ),null);
                            assertTrue(Objects.get(List.class,m,"list").size()>0);
                            if(input==0)
                                trace(Objects.get(m,"list"));


                        } catch (Throwable e) {
                            throw S1SystemError.wrap(e);
                        }
                return null;
            }
        }));

        //remove
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(final Integer input)  {
                        try {
                            String id = ids.get(input);
                            Map<String, Object> m = null;
                            boolean b = false;

                            //remove

                            m = client().postJSON(getContext()+"/dispatcher/Table.remove",Objects.newHashMap(String.class,Object.class,
                                    "table","table1",
                                    "action","remove",
                                    "id",id),null);

                            assertEquals(id, Objects.get(m, "id"));
                            assertEquals("a_" + input, Objects.get(m, "a"));
                            assertEquals(input, Objects.get(Integer.class,m, "b"));

                            try{
                                m = client().postJSON(getContext() + "/dispatcher/Table.get", Objects.newHashMap(String.class, Object.class,
                                        "table","table1",
                                        "id", id
                                ), null);
                            }catch (RuntimeException e){
                                b = true;
                            }
                            assertTrue(b);

                        } catch (Throwable e) {
                            throw S1SystemError.wrap(e);
                        }

                return null;
            }
        }));
    }

}
