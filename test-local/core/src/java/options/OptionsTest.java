package options;

import org.s1.format.json.JSONFormatException;
import org.s1.format.xml.XMLFormatException;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.s1.testing.BasicTest;
import org.s1.testing.LoadTestUtils;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 17.01.14
 * Time: 15:11
 */
public class OptionsTest extends BasicTest {

    @Test 
	public void testGetParameter(){
        int p = 10;
        title("Get parameter, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception {
                if (index == 0)
                    trace(Options.getStorage().getMap("System"));
                assertEquals("qwer", Options.getStorage().getSystem("test1"));
                assertEquals("asd", Options.getStorage().getSystem("m1.a"));
                assertEquals(123, Options.getStorage().getSystem(Integer.class, "m1.i").intValue());
                assertEquals(true, Options.getStorage().getSystem("m1.b"));
                assertEquals(2, Options.getStorage().getSystem(List.class, "l").size());
                assertEquals("asd", Options.getStorage().getSystem("l[0].a"));
                assertEquals(123, Options.getStorage().getSystem(Integer.class, "l[0].i").intValue());
                assertEquals(true, Options.getStorage().getSystem("l[0].b"));
                assertEquals("asdf", Options.getStorage().getSystem("l[1]"));

                
            }
        }));
    }

    @Test 
	public void testGetMap(){
        int p = 1;
        title("Get map, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception {

                if (index == 0)
                    trace(Options.getStorage().getMap("System"));
                assertEquals("qwer", Objects.get(Options.getStorage().getMap("System"), "test1"));
                assertEquals("asd", Objects.get(Options.getStorage().getMap("System"), "m1.a"));
                assertEquals(123, Objects.get(Integer.class, Options.getStorage().getMap("System"), "m1.i").intValue());
                assertEquals(true, Objects.get(Options.getStorage().getMap("System"), "m1.b"));
                assertEquals(2, Objects.get(List.class, Options.getStorage().getMap("System"), "l").size());
                assertEquals("asd", Objects.get(Options.getStorage().getMap("System"), "l[0].a"));
                assertEquals(123, Objects.get(Integer.class, Options.getStorage().getMap("System"), "l[0].i").intValue());
                assertEquals(true, Objects.get(Options.getStorage().getMap("System"), "l[0].b"));
                assertEquals("asdf", Objects.get(Options.getStorage().getMap("System"), "l[1]"));

                
            }
        }));
    }

    @Test 
	public void testFormat(){
        int p = 10;
        title("Format, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception {

                Map<String, Object> m = Objects.newHashMap("a", 1, "b", "2", "l", Objects.newArrayList(Objects.newHashMap("a", "1"), "aaa"), "m", Objects.newHashMap("a", "2"));
                String s = Options.getStorage().formatMap(m);
                if (index == 0)
                    trace(s);
                assertTrue(s.contains("<b>2</b>"));
                assertTrue(s.contains("<i:a>1</i:a>"));
                assertTrue(s.contains("<list:l>"));
                assertTrue(s.contains("</list:l>"));
                assertTrue(s.contains("<map:e>"));
                assertTrue(s.contains("<map:m>"));
                assertTrue(s.contains("<a>1</a>"));
                assertTrue(s.contains("<a>2</a>"));
                assertTrue(s.endsWith("</map:cfg>"));
                
            }
        }));
    }

    @Test 
	public void testParse(){
        int p = 10;
        title("Get parameter, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception {

                String s1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<!--\n" +
                        "~ Copyright 2013-${new Date().format(\"yyyy\")} S1 Platform\n" +
                        "~ Created at: ${new Date().format(\"yyyy-MM-dd HH:mm:ss\")}\n" +
                        "-->\n" +
                        "\n" +
                        "<map:cfg\n" +
                        "  xmlns:bi=\"http://s1-platform.org/config/types/BigInteger\"\n" +
                        "  xmlns:bd=\"http://s1-platform.org/config/types/BigDecimal\"\n" +
                        "  xmlns:i=\"http://s1-platform.org/config/types/Integer\"\n" +
                        "  xmlns:l=\"http://s1-platform.org/config/types/Long\"\n" +
                        "  xmlns:f=\"http://s1-platform.org/config/types/Float\"\n" +
                        "  xmlns:d=\"http://s1-platform.org/config/types/Double\"\n" +
                        "  xmlns:dt=\"http://s1-platform.org/config/types/Date\"\n" +
                        "  xmlns:b=\"http://s1-platform.org/config/types/Boolean\"\n" +
                        "  xmlns:map=\"http://s1-platform.org/config/types/Map\"\n" +
                        "  xmlns:list=\"http://s1-platform.org/config/types/List\">\n" +
                        "\n" +
                        "    <b>2</b>\n" +
                        "    <i:a>1</i:a>\n" +
                        "    <list:l>\n" +
                        "      <map:e>\n" +
                        "        <a>1</a>\n" +
                        "      </map:e>\n" +
                        "      <e>aaa</e>\n" +
                        "    </list:l>\n" +
                        "    <map:m>\n" +
                        "      <a>2</a>\n" +
                        "    </map:m>\n" +
                        "</map:cfg>";
                String s2 = "{\"b\":\"2\",\"a\":1,\"l\":[{\"a\":\"1\"},\"aaa\"],\"m\":{\"a\":\"2\"}}";


                Map<String, Object> m1 = null;
                Map<String, Object> m2 = null;
                try {
                    m1 = Options.getStorage().parseToMap(s1);
                    m2 = Options.getStorage().parseToMap(s2);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                try {
                    Options.getStorage().parseToMap("{asd:1}");
                    throw new RuntimeException("error");
                } catch (JSONFormatException e) {
                    if (index == 0)
                        trace(e.getMessage());
                } catch (XMLFormatException e) {
                    throw new RuntimeException(e);
                }

                try {
                    Options.getStorage().parseToMap("<s>}");
                    throw new RuntimeException("error");
                } catch (XMLFormatException e) {
                    if (index == 0)
                        trace(e.getMessage());
                } catch (JSONFormatException e) {
                    throw new RuntimeException(e);
                }

                if (index == 0)
                    trace(m1 + ":::" + m2);

                assertTrue(Objects.equals(m1, m2));

                
            }
        }));
    }

    @Test
	public void testReadConfig(){
        int p = 10;
        title("Read config, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception {
                assertEquals("test", Options.getStorage().readConfigToString("test.txt"));


            }
        }));
    }

}
