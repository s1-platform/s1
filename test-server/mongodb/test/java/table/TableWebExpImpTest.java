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
import com.mongodb.DBCollection;
import org.s1.S1SystemError;
import org.s1.misc.IOUtils;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.mongodb.MongoDBFormat;
import org.s1.objects.Objects;
import org.s1.table.Table;
import org.s1.testing.HttpServerTest;
import org.s1.testing.LoadTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 21.02.14
 * Time: 10:57
 */
public class TableWebExpImpTest extends HttpServerTest {

    @BeforeMethod
    protected void clear() throws Exception {
        TestTable1 t = new TestTable1();
        MongoDBConnectionHelper.getConnection(t.getCollectionId().getDatabase())
                .getCollection(t.getCollectionId().getCollection()).remove(new BasicDBObject());
        trace("Cleared");
    }

    @Test
    public void testExpImp() {
        final int p = 10;
        final int c = 10;
        //add
        TestTable1 t = new TestTable1();
        try{
            DBCollection coll = MongoDBConnectionHelper.getConnection(t.getCollectionId().getDatabase()).getCollection(t.getCollectionId().getCollection());
            for(int i=0;i<c;i++){
                coll.insert(MongoDBFormat.fromMap(Objects.newHashMap(String.class, Object.class,
                        "id", "" + i,
                        "a", "test_" + i,
                        "b", i
                )));
            }
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }

        //export
        final List<List<Map<String,Object>>> exports = Objects.newArrayList();
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input)  throws Exception {
                Map<String, Object> m = null;

                //add
                m = client().postJSON(getContext()+"/dispatcher/Table.list",Objects.newHashMap(String.class,Object.class,
                        "table","table1",
                        "search",Objects.newHashMap(
                                "b",1
                            )));

                List<Map<String,Object>> e = Objects.get(m,"result");
                assertEquals(1,e.size());
                synchronized (exports) {
                    exports.add(e);
                }

            }
        }));

        //delete all
        MongoDBConnectionHelper.getConnection(t.getCollectionId().getDatabase())
                .getCollection(t.getCollectionId().getCollection()).remove(new BasicDBObject());

        try{
            List<Map<String,Object>> list = Objects.newArrayList();
            list.addAll(t.list(null,null,null,0,10));
            assertEquals(0L,t.count(null));
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }

        //import new
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input)  throws Exception {
                Map<String,Object> m = null;
                for(Map<String,Object> e:exports.get(input)){
                    e.remove("id");
                }
                //import
                m = client().postJSON(getContext()+"/dispatcher/Table.doImport",Objects.newHashMap(String.class,Object.class,
                        "table","table1",
                        "list",exports.get(input)));
                int s = Objects.get(Integer.class,m,"result");
                assertTrue(1>=s);

            }
        }));

        try{
            List<Map<String,Object>> list = Objects.newArrayList();
            list.addAll(t.list(null,null,null,0,10));
            assertEquals(1L,t.count(null));
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

}
