package table;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.s1.S1SystemError;
import org.s1.cluster.Session;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.misc.FileUtils;
import org.s1.misc.IOUtils;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.s1.table.Table;
import org.s1.table.TablesFactory;
import org.s1.table.format.Sort;
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
        MongoDBConnectionHelper.getConnection(null)
                .getCollection(Options.getStorage().get(String.class, "table/test1", "collection")).remove(new BasicDBObject());
        trace("Cleared");
    }

    public void testExpImp() {
        final int p = 1;
        final int c = 10;
        title("Export import, parallel: " + p);
        //add
        try{
            for(int i=0;i<c;i++){
                TablesFactory.getTable("test1").changeState(null,"add",Objects.newHashMap(String.class,Object.class,
                        "a","test_"+i,
                        "b",i
                        ),null);
            }
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }

        //export
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(final Integer input) throws ClosureException {
                Map<String, Object> m = null;

                //add
                m = client().postJSON(getContext()+"/dispatcher/ExpImp.exportData",Objects.newHashMap(String.class,Object.class,
                        "table","test1",
                        "group","test1",
                        "type","default",
                        "search",Objects.newHashMap(
                            "node",Objects.newHashMap(
                                "field","b",
                                "operation","equals",
                                "value",1
                    ))),null);
                assertEquals("test1",m.get("group"));
                assertNotNull(m.get("id"));

                //download
                try{
                    FileOutputStream fos = new FileOutputStream(new File("/var/exp_"+input));
                    fos.write(client().get(getContext() + "/dispatcher/Upload.download", Objects.newHashMap(String.class, Object.class,
                            "group", "test1",
                            "id", m.get("id")), null, null).getData());
                    fos.close();
                }catch (Exception e){
                    throw S1SystemError.wrap(e);
                }
                return null;
            }
        }));

        //delete all
        MongoDBConnectionHelper.getConnection(null).getCollection(TablesFactory.getTable("test1").getCollection()).remove(new BasicDBObject());

        try{
            List<Map<String,Object>> list = Objects.newArrayList();
            assertEquals(0L,TablesFactory.getTable("test1").list(list,null,null,null,null,0,10));
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }

        //import new
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(final Integer input) throws ClosureException {
                Map<String,Object> m = null;
                //upload
                String id = "";
                try{
                    FileInputStream fis = new FileInputStream(new File("/var/exp_0"));
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    IOUtils.copy(fis,bos);
                    m = client().uploadFileForJSON(getContext() + "/dispatcher/Upload.upload?group=test2", new ByteArrayInputStream(bos.toByteArray()),
                            "export.zip",
                            "application/zip",null);
                    id = Objects.get(m,"id");
                    assertNotNull(id);
                    fis.close();
                }catch (Exception e){
                    throw S1SystemError.wrap(e);
                }

                //preview
                m = client().postJSON(getContext()+"/dispatcher/ExpImp.viewData",Objects.newHashMap(String.class,Object.class,
                        "group","test2",
                        "type","default",
                        "id",id),null);
                List<Map<String,Object>> l = Objects.get(m,"list");
                assertNotNull(Objects.get(m,"schema"));
                assertEquals(1L,Objects.get(m,"count"));
                assertEquals(1,l.size());
                assertEquals("test_1",Objects.get(l.get(0),"a"));
                assertEquals(1,Objects.get(Integer.class,l.get(0),"b").intValue());

                //import
                m = client().postJSON(getContext()+"/dispatcher/ExpImp.importData",Objects.newHashMap(String.class,Object.class,
                        "table","test1",
                        "group","test2",
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
            assertEquals(1L,TablesFactory.getTable("test1").list(list,null,null,null,null,0,10));
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

}
