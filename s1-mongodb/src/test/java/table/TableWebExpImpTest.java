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
import org.s1.misc.Closure;
import org.s1.misc.IOUtils;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.s1.table.Table;
import org.s1.test.LoadTestUtils;
import org.s1.test.ServerTest;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 21.02.14
 * Time: 10:57
 */
public class TableWebExpImpTest extends ServerTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Table t = new TestTable1();
        MongoDBConnectionHelper.getConnection(t.getCollectionId().getDatabase())
                .getCollection(t.getCollectionId().getCollection()).remove(new BasicDBObject());
        trace("Cleared");
    }

    public void testExpImp() {
        final int p = 1;
        final int c = 10;
        title("Export import, parallel: " + p);
        //add
        Table t = new TestTable1();
        try{
            for(int i=0;i<c;i++){
                t.changeState(null, "add", Objects.newHashMap(String.class, Object.class,
                        "a", "test_" + i,
                        "b", i
                ));
            }
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }

        //export
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(final Integer input)  {
                Map<String, Object> m = null;

                //add
                m = client().postJSON(getContext()+"/dispatcher/TableExpImp.exportData",Objects.newHashMap(String.class,Object.class,
                        "table","table.TestTable1",
                        "collection","test1",
                        "type","default",
                        "search",Objects.newHashMap(
                            "node",Objects.newHashMap(
                                "field","b",
                                "operation","equals",
                                "value",1
                    ))),null);
                assertNotNull(m.get("id"));

                //download
                try{
                    FileOutputStream fos = new FileOutputStream(new File("/var/exp_"+input));
                    fos.write(client().get(getContext() + "/dispatcher/Upload.download", Objects.newHashMap(String.class, Object.class,
                            "collection", "test1",
                            "id", m.get("id")), null, null).getData());
                    fos.close();
                }catch (Exception e){
                    throw S1SystemError.wrap(e);
                }
                return null;
            }
        }));

        //delete all
        MongoDBConnectionHelper.getConnection(t.getCollectionId().getDatabase())
                .getCollection(t.getCollectionId().getCollection()).remove(new BasicDBObject());

        try{
            List<Map<String,Object>> list = Objects.newArrayList();
            assertEquals(0L,t.list(list,null,null,null,0,10));
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }

        //import new
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(final Integer input)  {
                Map<String,Object> m = null;
                //upload
                String id = "";
                try{
                    FileInputStream fis = new FileInputStream(new File("/var/exp_0"));
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    IOUtils.copy(fis,bos);
                    m = client().uploadFileForJSON(getContext() + "/dispatcher/Upload.upload?collection=test2", new ByteArrayInputStream(bos.toByteArray()),
                            "export.zip",
                            "application/zip",null);
                    id = Objects.get(m,"id");
                    assertNotNull(id);
                    fis.close();
                }catch (Exception e){
                    throw S1SystemError.wrap(e);
                }

                //preview
                m = client().postJSON(getContext()+"/dispatcher/TableExpImp.viewData",Objects.newHashMap(String.class,Object.class,
                        "collection","test2",
                        "type","default",
                        "id",id),null);
                List<Map<String,Object>> l = Objects.get(m,"list");
                assertNotNull(Objects.get(m,"schema"));
                assertEquals(1L,Objects.get(m,"count"));
                assertEquals(1,l.size());
                assertEquals("test_1",Objects.get(l.get(0),"a"));
                assertEquals(1,Objects.get(Integer.class,l.get(0),"b").intValue());

                //import
                m = client().postJSON(getContext()+"/dispatcher/TableExpImp.importData",Objects.newHashMap(String.class,Object.class,
                        "table","table.TestTable1",
                        "collection","test2",
                        "type","default",
                        "id",id),null);
                l = Objects.get(m,"list");
                assertEquals(1,l.size());
                assertTrue(Objects.get(Boolean.class,l.get(0),"success"));

                return null;
            }
        }));

        try{
            List<Map<String,Object>> list = Objects.newArrayList();
            assertEquals(1L,t.list(list,null,null,null,0,10));
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

}
