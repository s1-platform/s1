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
import org.s1.S1SystemError;
import org.s1.cluster.Locks;
import org.s1.cluster.dds.beans.CollectionId;
import org.s1.cluster.dds.beans.Id;
import org.s1.misc.Closure;
import org.s1.mongodb.MongoDBFormat;
import org.s1.mongodb.MongoDBQueryHelper;
import org.s1.mongodb.cluster.MongoDBDDS;
import org.s1.objects.Objects;
import org.s1.table.errors.NotFoundException;
import org.s1.testing.ClusterTest;
import org.s1.testing.LoadTestUtils;
import org.s1.testing.HttpServerTest;
import org.testng.annotations.Test;

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

    @Test
    public void testAll(){
        int p=10;
        title("Add set remove, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input)  throws Exception {
                String id = "id_"+input;
                MongoDBDDS.add(new Id(null, COLL, id), Objects.newHashMap(String.class, Object.class,
                        "title", "test_" + input
                ));

                //
                Map<String,Object> m = null;
                try{
                    m = MongoDBQueryHelper.get(new CollectionId(null,COLL),Objects.newHashMap(String.class,Object.class,
                            "id",id
                    ));
                }catch (Exception e){
                    throw S1SystemError.wrap(e);
                }
                assertEquals(id,m.get("id"));
                assertEquals("test_"+input,m.get("title"));
                m.put("x","y");

                //set
                MongoDBDDS.set(new Id(null, COLL, id), m);

                try{
                    m = MongoDBQueryHelper.get(new CollectionId(null,COLL),Objects.newHashMap(String.class,Object.class,
                            "id",id
                    ));
                }catch (Exception e){
                    throw S1SystemError.wrap(e);
                }

                assertEquals(id,m.get("id"));
                assertEquals("test_"+input,m.get("title"));
                assertEquals("y",m.get("x"));

                //remove
                MongoDBDDS.remove(new Id(null, COLL, id));

                boolean b = false;
                try{
                    MongoDBQueryHelper.get(new CollectionId(null,COLL),Objects.newHashMap(String.class,Object.class,
                            "id",id
                    ));
                }catch (NotFoundException e){
                    b = true;
                }catch (Exception e){
                    throw S1SystemError.wrap(e);
                }

                assertTrue(b);
            }
        }));
    }

}
