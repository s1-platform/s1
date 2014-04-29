package ws;

import org.s1.S1SystemError;
import org.s1.format.xml.XMLFormat;
import org.s1.format.xml.XMLFormatException;
import org.s1.format.xml.XSDFormatException;
import org.s1.format.xml.XSDValidationException;
import org.s1.misc.Closure;

import org.s1.objects.Objects;


import org.s1.testing.HttpServerTest;
import org.s1.testing.LoadTestUtils;
import org.s1.ws.SOAPHelper;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

/**
 * s1v2
 * User: GPykhov
 * Date: 25.01.14
 * Time: 12:52
 */
public class SOAPHelperTest extends HttpServerTest {

    @Test
    public void testSend(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {
                String soap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "                <SOAP-ENV:Header> </SOAP-ENV:Header>\n" +
                        "                <SOAP-ENV:Body>\n" +
                        "                <cry:SignRequest xmlns:cry=\"http://example.com/\">\n" +
                        "                <type></type>\n" +
                        "                <key></key>\n" +
                        "                <data></data>\n" +
                        "                </cry:SignRequest>\n" +
                        "                </SOAP-ENV:Body>\n" +
                        "                </SOAP-ENV:Envelope>";
                SOAPMessage msg = SOAPHelper.createSoapFromString(soap);

                SOAPMessage resp = null;
                try {
                    resp = SOAPHelper.send("http://localhost:"+getPort()+getContext()+"/dispatcher/ws1", msg);
                } catch (SOAPException e) {
                    throw S1SystemError.wrap(e);
                }
                if(input==0)
                    trace(SOAPHelper.toString(resp));
                assertNotNull(XMLFormat.getElement(SOAPHelper.getEnvelope(resp), "Body.SignResponse.data", null));

            }
        }));
    }

    @Test
    public void testSendSoapAction(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {
                String soap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "                <SOAP-ENV:Header> </SOAP-ENV:Header>\n" +
                        "                <SOAP-ENV:Body>\n" +
                        "                <cry:SignRequest xmlns:cry=\"http://example.com/\">\n" +
                        "                <type></type>\n" +
                        "                <key></key>\n" +
                        "                <data></data>\n" +
                        "                </cry:SignRequest>\n" +
                        "                </SOAP-ENV:Body>\n" +
                        "                </SOAP-ENV:Envelope>";

                SOAPMessage msg = SOAPHelper.createSoapFromString(soap);
                msg.getMimeHeaders().setHeader("SOAPAction","urn:test1");
                try {
                    SOAPHelper.send("http://localhost:"+getPort()+getContext()+"/dispatcher/ws1", msg);
                } catch (SOAPException e) {
                    throw S1SystemError.wrap(e);
                }
            }
        }));
    }
}
