package format.json;

import org.s1.format.json.JSONFormat;
import org.s1.format.json.JSONFormatException;
import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.s1.test.BasicTest;
import org.s1.test.LoadTestUtils;

import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 09.01.14
 * Time: 21:05
 */
public class JSONFormatTest extends BasicTest {

    public void testEvalJSON() {
        int p = 10;
        title("Eval JSON, parallel " + p);
        assertEquals(p, LoadTestUtils.run("parsing", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) {
                Map<String, Object> m = null;
                try {
                    m = JSONFormat.evalJSON("{\"a1\":1,\"a2\":1.2,\"b1\":true,\"s\":\"ccc\",\"b\":{\"c\":[1,2,3]}}");
                } catch (JSONFormatException e) {
                    throw new RuntimeException(e);
                }
                if (index == 0)
                    trace(m.toString());

                assertEquals(1L, m.get("a1"));
                assertEquals(1.2D, m.get("a2"));
                assertEquals(true, m.get("b1"));
                assertTrue(m.get("a1") instanceof Long);
                assertTrue(m.get("a2") instanceof Double);
                assertEquals("ccc", m.get("s"));
                assertTrue(m.get("b") instanceof Map);
                assertTrue(((Map) m.get("b")).get("c") instanceof List);
                assertTrue(((List) ((Map) m.get("b")).get("c")).size() == 3);
                assertTrue(((List) ((Map) m.get("b")).get("c")).get(0).equals(1L));
                assertTrue(((List) ((Map) m.get("b")).get("c")).get(1).equals(2L));
                assertTrue(((List) ((Map) m.get("b")).get("c")).get(2).equals(3L));
                return null;
            }
        }));
    }

    public void testEvalJSONFromFile() {
        int p = 10;
        title("Eval JSON from file, parallel " + p);
        assertEquals(p, LoadTestUtils.run("parsing", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) {
                Map<String, Object> m = null;
                String json = resourceAsString("/format/json/test1.json");
                try {
                    m = JSONFormat.evalJSON(json);
                } catch (JSONFormatException e) {
                    throw new RuntimeException(e);
                }
                if (index == 0)
                    trace(m.toString());

                assertTrue(Objects.equals(m, Objects.newHashMap("a", 1, "b", "aaa", "c", Objects.newArrayList(1, 2, 3), "d", Objects.newHashMap("a", 1, "b", Objects.newArrayList("1", "2")))));
                return null;
            }
        }));
    }

    public void testToJSON() {
        int p = 10;
        title("To JSON, parallel " + p);
        final String json1 = "{\"a1\":1.0,\"a2\":1.2,\"b1\":true,\"s\":\"ccc\",\"b\":{\"c\":[1,2,3]}}";
        final Map<String, Object> m;
        try {
            m = JSONFormat.evalJSON(json1);
        } catch (JSONFormatException e) {
            throw new RuntimeException(e);
        }
        assertEquals(p, LoadTestUtils.run("formatting", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) {
                String json = JSONFormat.toJSON(m);
                if (index == 0)
                    trace(json);
                assertTrue(json.contains("\"a1\":1.0"));
                assertTrue(json.contains("\"a2\":1.2"));
                assertTrue(json.contains("\"b1\":true"));
                assertTrue(json.contains("\"s\":\"ccc\""));
                assertTrue(json.contains("\"b\":{\"c\":[1,2,3]}"));
                return null;
            }
        }));
    }

    public void testNewLine() {
        title("New line");
        final Map<String, Object> m = Objects.newHashMap("a","asd\n\t>aaa");
        Map<String, Object> m2 = null;
        try {
            m2 = JSONFormat.evalJSON(JSONFormat.toJSON(m));
        } catch (JSONFormatException e) {
            throw new RuntimeException(e);
        }
        assertTrue(Objects.equals(m,m2));
    }


}
