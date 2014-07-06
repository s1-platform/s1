package objects;

import org.s1.objects.EscapeUtils;
import org.s1.objects.Ranges;
import org.s1.testing.BasicTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Grigory Pykhov (grigoryp@hflabs.ru)
 */
public class EscapeTest extends BasicTest {

    @DataProvider(name = "data")
    protected Object[][] getLongsCases(){
        return new Object[][]{
                {"js","hello","hello"},
                {"js","hello \"test\"","hello \\\"test\\\""},
                {"js","hello \\ test","hello \\\\ test"},
                {"js","hello 'test'","hello \\'test\\'"},

                {"xml","<a>a&b 'test' \"test\"</a>","&lt;a&gt;a&amp;b &apos;test&apos; &quot;test&quot;&lt;/a&gt;"},
                {"html","<a>a&b 'test' \"test\"</a>","&lt;a&gt;a&amp;b &apos;test&apos; &quot;test&quot;&lt;/a&gt;"},
        };
    }

    @Test(dataProvider = "data")
    public void test(String type, String s, String res){
        if("js".equals(type))
            assertEquals(res, EscapeUtils.escapeJS(s));
        else if("html".equals(type))
            assertEquals(res, EscapeUtils.escapeHTML(s));
        else if("xml".equals(type))
            assertEquals(res, EscapeUtils.escapeXML(s));
    }
}
