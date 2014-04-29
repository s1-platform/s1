package format.xml;

import org.s1.format.xml.XMLFormat;
import org.s1.format.xml.XMLFormatException;
import org.s1.format.xml.XSDFormatException;
import org.s1.format.xml.XSDValidationException;
import org.s1.objects.Objects;
import org.s1.testing.BasicTest;
import org.s1.testing.LoadTestUtils;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 09.01.14
 * Time: 21:05
 */
public class XMLFormatTest extends BasicTest {

    @Test
    public void testString() {
        int p = 10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {

                String xmls = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><a><a:b xmlns:a=\"urn:a\" xxx=\"yyy\">qwer</a:b></a>";
                Document xml = null;
                try {
                    xml = XMLFormat.fromString(xmls);
                } catch (XMLFormatException e) {
                    throw new RuntimeException(e);
                }

                assertEquals(xmls, XMLFormat.toString(xml));
            }
        }));
    }

    @Test
    public void testGet() {
        int p = 1;
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {

                String xmls = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><a><a:b xmlns:a=\"urn:a\" xxx=\"yyy\" a:xx=\"zzz\"><a>qwer</a><c>aaa</c><c>aaa1</c><c>aaa2</c></a:b></a>";
                Document xml = null;
                try {
                    xml = XMLFormat.fromString(xmls);
                } catch (XMLFormatException e) {
                    throw new RuntimeException(e);
                }

                assertEquals("yyy", XMLFormat.get(xml, "b.@xxx", null));
                assertEquals("qwer", XMLFormat.get(xml, "b.a", null));
                assertEquals("aaa", XMLFormat.get(xml, "b.c[0]", null));
                assertEquals("aaa1", XMLFormat.get(xml, "b.c[1]", null));
                assertEquals("aaa2", XMLFormat.get(xml, "b.c[2]", null));
                assertEquals("qwer", XMLFormat.get(xml, "x:b.a", Objects.newHashMap(String.class, String.class, "x", "urn:a")));
                assertEquals(null, XMLFormat.get(xml, "x:b.a", Objects.newHashMap(String.class, String.class, "x", "urn:b")));
                assertEquals("zzz", XMLFormat.get(xml, "b.@x:xx", Objects.newHashMap(String.class, String.class, "x", "urn:a")));

                assertEquals("aaa", XMLFormat.getElement(xml, "b.c[0]", null).getTextContent());
                assertEquals("aaa1", XMLFormat.getElement(xml, "b.c[1]", null).getTextContent());
                assertEquals("aaa2", XMLFormat.getElement(xml, "b.c[2]", null).getTextContent());

                assertEquals("b", XMLFormat.getFirstChildElement(xml, null, null).getLocalName());
                assertEquals("b", XMLFormat.getFirstChildElement(xml, "b", null).getLocalName());
                assertEquals("b", XMLFormat.getFirstChildElement(xml, "b", "urn:a").getLocalName());

            }
        }));
    }

    @Test
    public void testValidate() {
        int p = 1;
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {
                String xmls = "<a a=\"qwer\" b=\"asdf\" тест=\"йцук\" xmlns:a=\"urn:a\">\n" +
                        "    <a>asdf</a>\n" +
                        "    <b>qwer</b>\n" +
                        "    <b1>\n" +
                        "        <a>aaa</a>\n" +
                        "    </b1>\n" +
                        "    <b2>\n" +
                        "        <a>aaa2</a>\n" +
                        "    </b2>\n" +
                        "    <c x=\"z0\" />\n" +
                        "    <c x=\"z1\" />\n" +
                        "    <тест>тест</тест>" +
                        "</a>";
                String xmls2 = "<a a=\"qwer\" b=\"asdf\" тест=\"йцук\" xmlns:a=\"urn:a\">\n" +
                        "    <b>qwer</b>\n" +
                        "    <b1>\n" +
                        "        <a>aaa</a>\n" +
                        "    </b1>\n" +
                        "    <b2>\n" +
                        "        <a>aaa2</a>\n" +
                        "    </b2>\n" +
                        "    <c x=\"z0\" />\n" +
                        "    <c x=\"z1\" />\n" +
                        "    <тест>тест</тест>" +
                        "</a>";
                Document xsd = null;
                Document xml = null;
                Document xml2 = null;
                try {
                    xsd = XMLFormat.fromString(resourceAsString("/format/xml/xsd_ref.xsd"));
                    xml = XMLFormat.fromString(xmls);
                    xml2 = XMLFormat.fromString(xmls2);
                } catch (XMLFormatException e) {
                    throw new RuntimeException(e);
                }

                try {
                    XMLFormat.validate(xsd, xml);
                } catch (XSDFormatException e) {
                    throw new RuntimeException(e);
                } catch (XSDValidationException e) {
                    throw new RuntimeException(e);
                }

                //error
                boolean b = false;
                try {
                    XMLFormat.validate(xsd, xml2);
                } catch (XSDFormatException e) {
                    throw new RuntimeException(e);
                } catch (XSDValidationException e) {
                    b=true;
                }
                assertTrue(b);

            }
        }));
    }

    @Test
    public void testToJSON() {
        int p = 10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {

                String xmls = "<a a=\"qwer\" a:b=\"asdf\" тест=\"йцук\" xmlns:a=\"urn:a\">\n" +
                        "    <a>asdf</a>\n" +
                        "    <b>qwer</b>\n" +
                        "    <c x=\"z0\" />\n" +
                        "    <c x=\"z1\" />\n" +
                        "    <a:тест>тест</a:тест>\n" +
                        "</a>";
                Document xml = null;
                try {
                    xml = XMLFormat.fromString(xmls);
                } catch (XMLFormatException e) {
                    throw new RuntimeException(e);
                }

                Map<String, Object> m = XMLFormat.toMap(xml);
                if (index == 0)
                    trace(m);
                assertEquals("qwer", Objects.get(m, "@a"));
                assertEquals("asdf", Objects.get(m, "@b"));
                assertEquals("йцук", Objects.get(m, "@тест"));
                assertEquals("asdf", Objects.get(m, "a"));
                assertEquals("qwer", Objects.get(m, "b"));
                assertEquals(2, Objects.get(List.class, m, "c").size());
                assertEquals("z0", Objects.get(m, "c[0].@x"));
                assertEquals("z1", Objects.get(m, "c[1].@x"));
                assertEquals("тест", Objects.get(m, "тест"));

            }

        }));
    }

    @Test
    public void testToJSONWithLists() {
        int p = 10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {

                String xmls = "<a a=\"qwer\" b=\"asdf\" тест=\"йцук\" xmlns:a=\"urn:a\">\n" +
                        "    <a>asdf</a>\n" +
                        "    <b>qwer</b>\n" +
                        "    <b1>\n" +
                        "        <a>aaa</a>\n" +
                        "    </b1>\n" +
                        "    <b2>\n" +
                        "        <a>aaa2</a>\n" +
                        "    </b2>\n" +
                        "    <c x=\"z0\" />\n" +
                        "    <c x=\"z1\" />\n" +
                        "    <тест>тест</тест>" +
                        "</a>";
                Document xsd = null;
                Document xml = null;
                try {
                    xsd = XMLFormat.fromString(resourceAsString("/format/xml/xsd1.xsd"));
                    xml = XMLFormat.fromString(xmls);
                } catch (XMLFormatException e) {
                    throw new RuntimeException(e);
                }

                try {
                    XMLFormat.validate(xsd, xml);
                } catch (XSDFormatException e) {
                    throw new RuntimeException(e);
                } catch (XSDValidationException e) {
                    throw new RuntimeException(e);
                }

                Map<String, Object> m = XMLFormat.toMap(xml, Objects.newArrayList(
                        "b", "b1", "b2.a", "c"
                ));
                if (index == 0)
                    trace(m);
                assertEquals("qwer", Objects.get(m, "@a"));
                assertEquals("asdf", Objects.get(m, "@b"));
                assertEquals("йцук", Objects.get(m, "@тест"));
                assertEquals("asdf", Objects.get(m, "a"));
                assertEquals(1, Objects.get(List.class, m, "b").size());
                assertEquals("qwer", Objects.get(m, "b[0]"));

                assertEquals(1, Objects.get(List.class, m, "b1").size());
                assertEquals("aaa", Objects.get(m, "b1[0].a"));
                assertEquals(1, Objects.get(List.class, m, "b2.a").size());
                assertEquals("aaa2", Objects.get(m, "b2.a[0]"));

                assertEquals(2, Objects.get(List.class, m, "c").size());
                assertEquals("z0", Objects.get(m, "c[0].@x"));
                assertEquals("z1", Objects.get(m, "c[1].@x"));
                assertEquals("тест", Objects.get(m, "тест"));

            }
        }));
    }
}
