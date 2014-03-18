package weboperation;

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
public class CommandTest extends ServerTest {

    public void testUploadDownload(){
        int p = 10;
        title("Command, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                List<Map<String,Object>> l = (List<Map<String,Object>>)client().postJSON(getContext()+"/dispatcher/Command",Objects.newHashMap(
                        String.class,Object.class,
                        "list",Objects.newArrayList(
                                Objects.newHashMap("operation","Echo","method","m","params",Objects.newHashMap("b",1)),
                                Objects.newHashMap("operation","Echo"),
                                Objects.newHashMap("operation","Echo","method","error")
                        )
                ),null).get("list");

                assertEquals(3,l.size());
                assertTrue(Objects.get(Boolean.class, l.get(0), "success"));
                assertTrue(Objects.get(Boolean.class, l.get(1), "success"));
                assertFalse(Objects.get(Boolean.class, l.get(2), "success"));
                assertEquals(1L,Objects.get(l.get(0),"data.b"));
                assertEquals("aaa",Objects.get(l.get(0),"data.a"));
                assertEquals("aaa",Objects.get(l.get(1),"data.a"));
                assertEquals("test error",Objects.get(l.get(2),"data.message"));
                return null;
            }
        }));
    }

}
