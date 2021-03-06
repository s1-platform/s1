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
import org.s1.testing.ClusterTest;
import org.s1.testing.LoadTestUtils;
import org.s1.testing.HttpServerTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.02.14
 * Time: 13:21
 */
public class QueryTest extends ClusterTest {

    private static final String COLL = "coll_query";

    @BeforeClass
    protected void prepareDB() throws Exception {
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
        /*coll.ensureIndex(new BasicDBObject(Objects.newHashMap(
                "str","text",
                "str2","text",
                "text","text"
        )));*/
        trace("data inserted");
    }

    @Test
    public void testGet(){
        int p=10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input)  throws Exception {

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
            }
        }));
    }

    @Test
    public void testList(){
        int p=10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input)  throws Exception {
                List<Map<String,Object>> res = MongoDBQueryHelper.list(new CollectionId(null,COLL),new BasicDBObject(
                        "index",Objects.newHashMap("$lte",50)),
                        new BasicDBObject("index",-1),
                        new BasicDBObject("str2",0),0,10);
                //assertEquals(51L,c);
                assertEquals(10,res.size());
                assertEquals(50,res.get(0).get("index"));
            }
        }));
    }

    /*public void testFullText(){
        int p=10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input)  throws Exception {
                List<Map<String,Object>> res = Objects.newArrayList();
                long c = MongoDBQueryHelper.fullTextSearch(res, new CollectionId(null,COLL), "test red", new BasicDBObject(
                        "index", Objects.newHashMap("$lte", 50)),
                        new BasicDBObject("str2", 0), 0, 10);
                assertEquals(51L,c);
                assertEquals(10,res.size());
                if(input==0)
                    trace(res);
            }
        }));
    }*/

}
