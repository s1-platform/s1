package org.s1.xsdutils.test;

import org.s1.format.xml.XMLFormat;
import org.s1.format.xml.XMLFormatException;
import org.s1.format.xml.XSDFormatException;
import org.s1.format.xml.XSDValidationException;
import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.s1.test.BasicTest;
import org.s1.test.LoadTestUtils;
import org.s1.xsdutils.XSD2ArraysList;
import org.s1.xsdutils.XSD2ObjectSchema;
import org.w3c.dom.Document;

import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 09.01.14
 * Time: 21:05
 */
public class XSDTest extends BasicTest {

    public void testToSchema() {
        int p = 1;
        title("To schema, parallel " + p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) {
                Document xsd = null;
                try {
                    xsd = XMLFormat.fromString(resourceAsString("/xsd1.xsd"));
                } catch (XMLFormatException e) {
                    throw new RuntimeException(e);
                }

                Map<String,Object> m = XSD2ObjectSchema.toSchemaMap(xsd);
                if(index==0)
                    trace(m);

                assertEquals(1,Objects.get(List.class,m,"attributes").size());
                assertEquals(9,Objects.get(List.class,m,"attributes[0].attributes").size());

                //elements
                assertEquals("a",Objects.get(m,"attributes[0].attributes[0].name"));
                assertEquals("e aaa",Objects.get(m,"attributes[0].attributes[0].label"));
                assertEquals("required",Objects.get(m,"attributes[0].attributes[0].appearance"));
                assertEquals("String",Objects.get(m,"attributes[0].attributes[0].type"));

                assertEquals("b",Objects.get(m,"attributes[0].attributes[1].name"));
                assertEquals("e bbb",Objects.get(m,"attributes[0].attributes[1].label"));
                assertEquals("required",Objects.get(m,"attributes[0].attributes[1].appearance"));
                assertEquals("List",Objects.get(m,"attributes[0].attributes[1].type"));
                assertEquals("String",Objects.get(m,"attributes[0].attributes[1].element.type"));
                assertEquals(1,Objects.get(m,"attributes[0].attributes[1].min"));

                assertEquals("b1",Objects.get(m,"attributes[0].attributes[2].name"));
                assertEquals("e b111",Objects.get(m,"attributes[0].attributes[2].label"));
                assertEquals("required",Objects.get(m,"attributes[0].attributes[2].appearance"));
                assertEquals("List",Objects.get(m,"attributes[0].attributes[2].type"));
                assertEquals("#ct2",Objects.get(m,"attributes[0].attributes[2].element.type"));
                assertNull(Objects.get(m,"attributes[0].attributes[2].min"));

                assertEquals("b2",Objects.get(m,"attributes[0].attributes[3].name"));
                assertEquals("e b222",Objects.get(m,"attributes[0].attributes[3].label"));
                assertEquals("normal",Objects.get(m,"attributes[0].attributes[3].appearance"));
                assertEquals("Map",Objects.get(m,"attributes[0].attributes[3].type"));
                assertEquals(1,Objects.get(List.class, m,"attributes[0].attributes[3].attributes").size());
                assertEquals("a",Objects.get(m,"attributes[0].attributes[3].attributes[0].name"));
                assertEquals("e aaa",Objects.get(m,"attributes[0].attributes[3].attributes[0].label"));
                assertEquals("required",Objects.get(m,"attributes[0].attributes[3].attributes[0].appearance"));
                assertEquals("List",Objects.get(m,"attributes[0].attributes[3].attributes[0].type"));
                assertEquals("String",Objects.get(m,"attributes[0].attributes[3].attributes[0].element.type"));
                assertEquals(1,Objects.get(m,"attributes[0].attributes[3].attributes[0].min"));
                assertEquals(2,Objects.get(m,"attributes[0].attributes[3].attributes[0].max"));

                assertEquals("c",Objects.get(m,"attributes[0].attributes[4].name"));
                assertEquals("e ccc",Objects.get(m,"attributes[0].attributes[4].label"));
                assertEquals("required",Objects.get(m,"attributes[0].attributes[4].appearance"));
                assertEquals("List",Objects.get(m,"attributes[0].attributes[4].type"));
                assertEquals("#ct1",Objects.get(m,"attributes[0].attributes[4].element.type"));
                assertEquals(1,Objects.get(m,"attributes[0].attributes[4].min"));

                assertEquals("тест",Objects.get(m,"attributes[0].attributes[5].name"));
                assertEquals("e ddd",Objects.get(m,"attributes[0].attributes[5].label"));
                assertEquals("required",Objects.get(m,"attributes[0].attributes[5].appearance"));
                assertEquals("String",Objects.get(m,"attributes[0].attributes[5].type"));

                //attributes
                assertEquals("@a",Objects.get(m,"attributes[0].attributes[6].name"));
                assertEquals("aaa",Objects.get(m,"attributes[0].attributes[6].label"));
                assertEquals("required",Objects.get(m,"attributes[0].attributes[6].appearance"));
                assertEquals("String",Objects.get(m,"attributes[0].attributes[6].type"));

                assertEquals("@b",Objects.get(m,"attributes[0].attributes[7].name"));
                assertEquals("bbb",Objects.get(m,"attributes[0].attributes[7].label"));
                assertEquals("normal",Objects.get(m,"attributes[0].attributes[7].appearance"));
                assertEquals("String",Objects.get(m,"attributes[0].attributes[7].type"));

                assertEquals("@тест",Objects.get(m,"attributes[0].attributes[8].name"));
                assertEquals("ccc",Objects.get(m,"attributes[0].attributes[8].label"));
                assertEquals("required",Objects.get(m,"attributes[0].attributes[8].appearance"));
                assertEquals("String",Objects.get(m,"attributes[0].attributes[8].type"));


                //types
                assertEquals(2,Objects.get(List.class,m,"types").size());

                //type1
                assertEquals("ct1",Objects.get(m,"types[0].name"));
                assertEquals(2,Objects.get(List.class,m,"types[0].attributes").size());
                assertEquals("element",Objects.get(m,"types[0].attributes[0].name"));
                assertEquals("element",Objects.get(m,"types[0].attributes[0].label"));
                assertEquals("normal",Objects.get(m,"types[0].attributes[0].appearance"));
                assertEquals("#ct1",Objects.get(m,"types[0].attributes[0].type"));
                assertEquals("@x",Objects.get(m,"types[0].attributes[1].name"));
                assertEquals("xxx",Objects.get(m,"types[0].attributes[1].label"));
                assertEquals("required",Objects.get(m,"types[0].attributes[1].appearance"));
                assertEquals("String",Objects.get(m,"types[0].attributes[1].type"));

                //type2
                assertEquals("ct2",Objects.get(m,"types[1].name"));
                assertEquals(2,Objects.get(List.class,m,"types[1].attributes").size());
                assertEquals("a",Objects.get(m,"types[1].attributes[0].name"));
                assertEquals("e aaa",Objects.get(m,"types[1].attributes[0].label"));
                assertEquals("required",Objects.get(m,"types[1].attributes[0].appearance"));
                assertEquals("String",Objects.get(m,"types[1].attributes[0].type"));
                assertEquals("b",Objects.get(m,"types[1].attributes[1].name"));
                assertEquals("e bbb",Objects.get(m,"types[1].attributes[1].label"));
                assertEquals("required",Objects.get(m,"types[1].attributes[1].appearance"));
                assertEquals("List",Objects.get(m,"types[1].attributes[1].type"));
                assertEquals("String",Objects.get(m,"types[1].attributes[1].element.type"));
                assertEquals(1,Objects.get(m,"types[1].attributes[1].min"));
                assertNull(Objects.get(m,"types[1].attributes[1].max"));

                return null;
            }
        }));
    }

    public void testToList() {
        int p = 1;
        title("To Arrays List, parallel " + p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) {
                Document xsd = null;
                try {
                    xsd = XMLFormat.fromString(resourceAsString("/xsd1.xsd"));
                } catch (XMLFormatException e) {
                    throw new RuntimeException(e);
                }

                List<String> l = XSD2ArraysList.toArraysList(xsd);
                if(index==0)
                    trace(l);

                assertEquals(5,l.size());
                assertEquals("a.b",l.get(0));
                assertEquals("a.b1",l.get(1));
                assertEquals("a.b2.a",l.get(2));
                assertEquals("a.c",l.get(3));
                assertEquals("#ct2.b",l.get(4));

                return null;
            }
        }));
    }

}
