package objects.schema;

import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;
import org.s1.test.LoadTestUtils;
import org.s1.test.ServerTest;

import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 10:45
 */
public class ObjectSchemaWebTest extends ServerTest {

    public void testValidate(){
        int p = 10;
        title("Validate, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                Map<String,Object> m = client().postJSON(getContext() + "/dispatcher/Schema.validate", Objects.newHashMap(
                        String.class, Object.class,
                        "schema", Objects.newHashMap("attributes",Objects.newArrayList(
                                Objects.newHashMap("name","a","label","a","appearance","required","type","String")
                        )),
                        "data", Objects.newHashMap("a","asd"),
                        "dataDiff",true,
                        "schemaDiff",true
                ), null);

                assertEquals("asd",Objects.get(m,"data.a"));
                assertEquals(0,Objects.get(List.class,m,"dataDiff").size());
                assertTrue(Objects.get(List.class,m,"schemaDiff").size()>0);
                assertNull(Objects.get(m,"schema.attributes[0].error"));

                //error
                m = client().postJSON(getContext() + "/dispatcher/Schema.validate", Objects.newHashMap(
                        String.class, Object.class,
                        "schema", Objects.newHashMap("attributes",Objects.newArrayList(
                        Objects.newHashMap("name","a","label","a","appearance","required","type","String")
                )),
                        "dataDiff",true,
                        "schemaDiff",true
                ), null);
                assertNotNull(Objects.get(m,"schema.attributes[0].error"));

                return null;
            }
        }));
    }

    public void testScriptValidate(){
        int p = 10;
        title("Script Validate, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                Map<String,Object> m = client().postJSON(getContext() + "/dispatcher/Schema.validate", Objects.newHashMap(
                        String.class, Object.class,
                        "schema", Objects.newHashMap("attributes",Objects.newArrayList(
                        Objects.newHashMap("name","a","label","a","appearance","required","type","String",
                                "validate","if(data=='qwer') throw 'err1';")
                )),
                        "data", Objects.newHashMap("a","asd"),
                        "dataDiff",true,
                        "schemaDiff",true
                ), null);

                assertEquals("asd",Objects.get(m,"data.a"));
                assertEquals(0,Objects.get(List.class,m,"dataDiff").size());
                assertTrue(Objects.get(List.class,m,"schemaDiff").size()>0);
                assertNull(Objects.get(m,"schema.attributes[0].error"));

                //error
                m = client().postJSON(getContext() + "/dispatcher/Schema.validate", Objects.newHashMap(
                        String.class, Object.class,
                        "schema", Objects.newHashMap("attributes",Objects.newArrayList(
                        Objects.newHashMap("name","a","label","a","appearance","required","type","String",
                                "validate","if(data=='qwer') throw 'err1';")
                )),
                        "data", Objects.newHashMap("a","qwer"),
                        "dataDiff",true,
                        "schemaDiff",true
                ), null);
                String err = Objects.get(m,"schema.attributes[0].error");
                assertNotNull(err);
                assertTrue(err.contains("err1"));
                if(input==0)
                    trace(err);

                return null;
            }
        }));
    }

}
