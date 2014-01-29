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

                assertEquals(6,Objects.get(List.class,m,"attributes").size());
                assertEquals(2,Objects.get(List.class,m,"types").size());

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

                assertEquals(4,l.size());

                return null;
            }
        }));
    }

}
