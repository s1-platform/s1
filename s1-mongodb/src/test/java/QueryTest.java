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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import org.s1.S1SystemError;
import org.s1.cluster.dds.beans.CollectionId;
import org.s1.misc.Closure;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.mongodb.MongoDBFormat;
import org.s1.mongodb.MongoDBQueryHelper;
import org.s1.objects.Objects;
import org.s1.table.errors.MoreThanOneFoundException;
import org.s1.table.errors.NotFoundException;
import org.s1.test.LoadTestUtils;
import org.s1.test.ServerTest;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.02.14
 * Time: 13:21
 */
public class QueryTest extends ServerTest {

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
                    "index", i,
                    "str", "test_" + i,
                    "str2", "test_" + (i % 50),
                    "text", "hello world test! "+(i%2==0?"red":"blue"),
                    "int", i,
                    "float", i / 10.0F,
                    "double", i / 100.0D,
                    "long", 100000L * i,
                    "date", new Date(t + (i * 1000) % 120),
                    "date2", new Date(t + i),
                    "bool", i % 3 == 0
            )));
        }
        coll.ensureIndex(new BasicDBObject(Objects.newHashMap(
                "str","text",
                "str2","text",
                "text","text"
        )));
        trace("data inserted");
    }

    public void testGet(){
        int p=10;
        title("Get, parallel:"+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input)  {

                Map<String,Object> m = null;
                try {
                    m = MongoDBQueryHelper.get(new CollectionId(null,COLL), new BasicDBObject(
                            "index", 10));
                } catch (NotFoundException e) {
                    e.printStackTrace();
                } catch (MoreThanOneFoundException e) {
                    e.printStackTrace();
                }
                assertEquals(10,m.get("index"));
                assertEquals("test_10",m.get("str"));

                //not found
                boolean b = false;
                try {
                    MongoDBQueryHelper.get(new CollectionId(null,COLL), new BasicDBObject(
                            "index", -10));
                } catch (NotFoundException e) {
                    b = true;
                } catch (MoreThanOneFoundException e) {
                    throw S1SystemError.wrap(e);
                }
                assertTrue(b);

                //>1
                b = false;
                try {
                    MongoDBQueryHelper.get(new CollectionId(null,COLL), null);
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
        int p=10;
        title("List, parallel:"+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input)  {
                List<Map<String,Object>> res = Objects.newArrayList();
                long c = MongoDBQueryHelper.list(res,new CollectionId(null,COLL),new BasicDBObject(
                        "index",Objects.newHashMap("$lte",50)),
                        new BasicDBObject("index",-1),
                        new BasicDBObject("str2",0),0,10);
                assertEquals(51L,c);
                assertEquals(10,res.size());
                assertEquals(50,res.get(0).get("index"));
                return null;
            }
        }));
    }

    /*public void testFullText(){
        int p=10;
        title("Full-text, parallel:"+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input)  {
                List<Map<String,Object>> res = Objects.newArrayList();
                long c = MongoDBQueryHelper.fullTextSearch(res, new CollectionId(null,COLL), "test red", new BasicDBObject(
                        "index", Objects.newHashMap("$lte", 50)),
                        new BasicDBObject("str2", 0), 0, 10);
                assertEquals(51L,c);
                assertEquals(10,res.size());
                if(input==0)
                    trace(res);
                return null;
            }
        }));
    }*/

}
