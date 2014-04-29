package ws;

import org.s1.S1SystemError;
import org.s1.format.xml.XMLFormat;
import org.s1.format.xml.XMLFormatException;
import org.s1.format.xml.XSDFormatException;
import org.s1.format.xml.XSDValidationException;
import org.s1.objects.Objects;
import org.s1.testing.BasicTest;
import org.s1.testing.LoadTestUtils;
import org.s1.ws.SOAPHelper;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

/**
 * s1v2
 * User: GPykhov
 * Date: 25.01.14
 * Time: 12:52
 */
public class SOAPHelperTest extends BasicTest {

    @Test
    public void testString(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {

                String soap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "                <SOAP-ENV:Header> </SOAP-ENV:Header>\n" +
                        "                <SOAP-ENV:Body>\n" +
                        "                <a:TestRequest xmlns:a=\"http://example.com/\">\n" +
                        "                <a>qwer0</a>\n" +
                        "                <a>тест1</a>\n" +
                        "                </a:TestRequest>\n" +
                        "                </SOAP-ENV:Body>\n" +
                        "                </SOAP-ENV:Envelope>";
                soap = soap.replace("\n",System.lineSeparator());

                SOAPMessage msg = SOAPHelper.createSoapFromString(soap);
                SOAPMessage msg2 = SOAPHelper.createSoapFromStream(Objects.newHashMap(String.class,String.class),new ByteArrayInputStream(soap.getBytes(Charset.forName("UTF-8"))));
                assertNotNull(msg);
                assertNotNull(msg2);
                assertTrue(SOAPHelper.getEnvelope(msg).isEqualNode(SOAPHelper.getEnvelope(msg2)));
                //soap
                if(input==0){
                    trace(SOAPHelper.toString(msg));
                    trace(SOAPHelper.toString(msg2));
                    trace(soap);
                }
                assertEquals(soap, SOAPHelper.toString(msg2));
                assertEquals(soap, SOAPHelper.toString(msg));

            }
        }));
    }

    @Test
    public void testFile(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {
                String soap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "                <SOAP-ENV:Header> </SOAP-ENV:Header>\n" +
                        "                <SOAP-ENV:Body>\n" +
                        "                <cry:SignRequest xmlns:cry=\"http://s1-platform.com/crypto\">\n" +
                        "                <type>smev</type>\n" +
                        "                <key>test</key>\n" +
                        "                <data>cXdlcg==</data>\n" +
                        "                <data2/>\n" +
                        "                </cry:SignRequest>\n" +
                        "                </SOAP-ENV:Body>\n" +
                        "                </SOAP-ENV:Envelope>";

                SOAPMessage msg = SOAPHelper.createSoapFromString(soap);
                assertNotNull(XMLFormat.getElement(SOAPHelper.getEnvelope(msg),"Body.SignRequest.data2",null));
                SOAPHelper.writeFile(msg.getAttachments().hasNext(),msg,XMLFormat.getElement(SOAPHelper.getEnvelope(msg),"Body.SignRequest.data2",null),"asdf".getBytes());
                assertEquals("qwer",new String(SOAPHelper.readFile(msg,XMLFormat.getElement(SOAPHelper.getEnvelope(msg),"Body.SignRequest.data",null))));
                assertEquals("asdf",new String(SOAPHelper.readFile(msg,XMLFormat.getElement(SOAPHelper.getEnvelope(msg),"Body.SignRequest.data2",null))));

                //mtom
                String mimeSoap = resourceAsString("/ws/mime.txt");
                SOAPMessage mimeMsg = SOAPHelper.createSoapFromStream(Objects.newHashMap(
                        String.class,String.class,
                        "Content-Type",
                        "multipart/related; type=\"application/xop+xml\"; start=\"<rootpart@soapui.org>\"; start-info=\"text/xml\"; boundary=\"----=_Part_96_20122774.1369204635982\""
                ),
                new ByteArrayInputStream(mimeSoap.getBytes()));

                SOAPHelper.writeFile(mimeMsg.getAttachments().hasNext(),mimeMsg,XMLFormat.getElement(SOAPHelper.getEnvelope(mimeMsg),"Body.SignRequest.data2",null),"asdf".getBytes());
                if(input==0)
                    trace(SOAPHelper.toString(mimeMsg));
                assertEquals("qwer",new String(SOAPHelper.readFile(mimeMsg,XMLFormat.getElement(SOAPHelper.getEnvelope(mimeMsg),"Body.SignRequest.data",null))));
                assertEquals("asdf",new String(SOAPHelper.readFile(mimeMsg,XMLFormat.getElement(SOAPHelper.getEnvelope(mimeMsg),"Body.SignRequest.data2",null))));

            }
        }));
    }

    @Test
    public void testValidate(){
        int p = 1;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {
                String soap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "                <SOAP-ENV:Header> </SOAP-ENV:Header>\n" +
                        "                <SOAP-ENV:Body>\n" +
                        "                <cry:SignRequest xmlns:cry=\"http://example.com/\">\n" +
                        "                <type>smev</type>\n" +
                        "                <key>test</key>\n" +
                        "                <data>cXdlcg==</data>\n" +
                        "                </cry:SignRequest>\n" +
                        "                </SOAP-ENV:Body>\n" +
                        "                </SOAP-ENV:Envelope>";
                SOAPMessage msg = SOAPHelper.createSoapFromString(soap);
                SOAPHelper.writeFile(true,msg,XMLFormat.getElement(SOAPHelper.getEnvelope(msg),"Body.SignRequest.data",null),"asdf".getBytes());

                String res = resourceAsString("/ws/wsdl.xml");
                try {
                    SOAPHelper.validateMessage(XMLFormat.fromString(res),msg);
                } catch (Exception e) {
                    throw S1SystemError.wrap(e);
                }
                //file ok
                assertEquals("asdf",new String(SOAPHelper.readFile(msg,XMLFormat.getElement(SOAPHelper.getEnvelope(msg),"Body.SignRequest.data",null))));


                //error
                String soap2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "                <SOAP-ENV:Header> </SOAP-ENV:Header>\n" +
                        "                <SOAP-ENV:Body>\n" +
                        "                <cry:SignRequest xmlns:cry=\"http://example.com/\">\n" +
                        "                <key>test</key>\n" +
                        "                <data>cXdlcg==</data>\n" +
                        "                </cry:SignRequest>\n" +
                        "                </SOAP-ENV:Body>\n" +
                        "                </SOAP-ENV:Envelope>";
                SOAPMessage msg2 = SOAPHelper.createSoapFromString(soap2);
                SOAPHelper.writeFile(true,msg2,XMLFormat.getElement(SOAPHelper.getEnvelope(msg2),"Body.SignRequest.data",null),"asdf".getBytes());

                boolean b = false;
                try {
                    SOAPHelper.validateMessage(XMLFormat.fromString(res),msg2);
                } catch (XSDFormatException e) {
                    throw S1SystemError.wrap(e);
                } catch (XSDValidationException e) {
                    //ok
                    b=true;
                } catch (XMLFormatException e) {
                    throw S1SystemError.wrap(e);
                }
                assertTrue(b);

                //file ok
                assertEquals("asdf",new String(SOAPHelper.readFile(msg2,XMLFormat.getElement(SOAPHelper.getEnvelope(msg2),"Body.SignRequest.data",null))));

            }
        }));
    }

    @Test
    public void testChange(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {
                SOAPMessage msg = SOAPHelper.createSoapFromString("<?xml version='1.0' encoding='UTF-8'?>\n" +
                        "                <SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
                        "                xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"\n" +
                        "                xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"\n" +
                        "                xmlns:smev=\"http://smev.gosuslugi.ru/rev120315\"\n" +
                        "                xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                        "                <SOAP-ENV:Header></SOAP-ENV:Header>\n" +
                        "                <SOAP-ENV:Body wsu:Id=\"body\">\n" +
                        "                <tns:aaa xmlns:tns=\"urn:a\">\n" +
                        "                <smev:MessageData>\n" +
                        "                <smev:AppData wsu:Id=\"AppData\">\n" +
                        "                </smev:AppData>\n" +
                        "                </smev:MessageData>\n" +
                        "                </tns:aaa>\n" +
                        "                </SOAP-ENV:Body>\n" +
                        "                </SOAP-ENV:Envelope>");

                Document temp_xml = null;
                try {
                    temp_xml = XMLFormat.fromString("<a>qwe</a>");
                } catch (XMLFormatException e) {
                    throw S1SystemError.wrap(e);
                }

                Element el = XMLFormat.getElement(SOAPHelper.getEnvelope(msg), "Body.aaa.MessageData.AppData", null);

                Node n = temp_xml.getDocumentElement().cloneNode(true);
                n = el.getOwnerDocument().importNode(n,true);
                if(input==0)
                    trace(n);
                el.appendChild(n);
                if(input==0)
                    trace(SOAPHelper.toString(msg));

                assertEquals("qwe",XMLFormat.get(SOAPHelper.getEnvelope(msg),"Body.aaa.MessageData.AppData.a",null));
            }
        }));
    }

}
