package ws;

import org.s1.S1SystemError;
import org.s1.format.xml.XMLFormat;
import org.s1.misc.Closure;

import org.s1.misc.IOUtils;


import org.s1.testing.HttpServerTest;
import org.s1.testing.LoadTestUtils;
import org.s1.ws.SOAPHelper;
import org.testng.annotations.Test;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.IOException;

/**
 * s1v2
 * User: GPykhov
 * Date: 25.01.14
 * Time: 12:52
 */
public class SOAPOperationTest extends HttpServerTest {

    @Test
    public void testResources(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {

                String s = null;
                s = IOUtils.toString(client().get(getContext() + "/dispatcher/ws1?wsdl", null, null).getData(),"UTF-8");

                if(input==0)
                    trace(s);
                assertTrue(s.contains("<wsdl:definitions "));
                assertTrue(s.contains("<xs:include schemaLocation=\"http://localhost:"+getPort()+getContext()+"/dispatcher/ws1?resource=schema.xsd\""));
                assertTrue(s.contains("<soap:address location=\"http://localhost:"+getPort()+getContext()+"/dispatcher/ws1\""));

                String s1 = null;
                s1 = IOUtils.toString(client().get(getContext() + "/dispatcher/ws1?resource=schema.xsd", null, null).getData(),"UTF-8");

                if(input==0)
                    trace(s1);
                assertTrue(s1.contains("<xs:schema "));

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
                        "                <cry:SignRequest xmlns:cry=\"http://example.com/\">\n" +
                        "                <type>smev</type>\n" +
                        "                <key>test</key>\n" +
                        "                <data>cXdlcg==</data>\n" +
                        "                </cry:SignRequest>\n" +
                        "                </SOAP-ENV:Body>\n" +
                        "                </SOAP-ENV:Envelope>";

                SOAPMessage msg = SOAPHelper.createSoapFromString(soap);
                assertNotNull(XMLFormat.getElement(SOAPHelper.getEnvelope(msg), "Body.SignRequest.data", null));
                SOAPHelper.writeFile(false, msg, XMLFormat.getElement(SOAPHelper.getEnvelope(msg), "Body.SignRequest.data", null), "asdf".getBytes());
                assertEquals("asdf",new String(SOAPHelper.readFile(msg,XMLFormat.getElement(SOAPHelper.getEnvelope(msg),"Body.SignRequest.data",null))));
                //send
                SOAPMessage res = null;
                try {
                    res = SOAPHelper.send("http://localhost:"+getPort()+getContext() + "/dispatcher/ws1", msg);
                } catch (SOAPException e) {
                    throw S1SystemError.wrap(e);
                }
                assertEquals("asdf1",new String(SOAPHelper.readFile(res,XMLFormat.getElement(SOAPHelper.getEnvelope(res),"Body.SignResponse.data",null))));


                //mtom
                msg = SOAPHelper.createSoapFromString(soap);
                assertNotNull(XMLFormat.getElement(SOAPHelper.getEnvelope(msg), "Body.SignRequest.data", null));
                SOAPHelper.writeFile(true, msg, XMLFormat.getElement(SOAPHelper.getEnvelope(msg), "Body.SignRequest.data", null), "asdf".getBytes());
                assertEquals("asdf",new String(SOAPHelper.readFile(msg,XMLFormat.getElement(SOAPHelper.getEnvelope(msg),"Body.SignRequest.data",null))));
                //send
                try {
                    res = SOAPHelper.send("http://localhost:"+getPort()+getContext() + "/dispatcher/ws1", msg);
                } catch (SOAPException e) {
                    throw S1SystemError.wrap(e);
                }
                assertEquals("asdf1",new String(SOAPHelper.readFile(res,XMLFormat.getElement(SOAPHelper.getEnvelope(res),"Body.SignResponse.data",null))));

            }
        }));
    }

    @Test
    public void testFault(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {
                String soap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "                <SOAP-ENV:Header> </SOAP-ENV:Header>\n" +
                        "                <SOAP-ENV:Body>\n" +
                        "                <cry:SignRequest1 xmlns:cry=\"http://example.com/\">\n" +
                        "                <type>smev</type>\n" +
                        "                <key>test</key>\n" +
                        "                <data>cXdlcg==</data>\n" +
                        "                </cry:SignRequest1>\n" +
                        "                </SOAP-ENV:Body>\n" +
                        "                </SOAP-ENV:Envelope>";

                SOAPMessage msg = SOAPHelper.createSoapFromString(soap);
                //send
                SOAPMessage res = null;
                try {
                    res = SOAPHelper.send("http://localhost:"+getPort()+getContext() + "/dispatcher/ws1", msg);
                } catch (SOAPException e) {
                    throw S1SystemError.wrap(e);
                }
                if(input==0)
                    trace(res);
                assertEquals("Server",XMLFormat.get(SOAPHelper.getEnvelope(res),
                        "Body.Fault.faultcode",null));
                assertEquals("java.lang.Exception: action",XMLFormat.get(SOAPHelper.getEnvelope(res),
                        "Body.Fault.faultstring",null));
                assertEquals("action",XMLFormat.get(SOAPHelper.getEnvelope(res),
                        "Body.Fault.detail.message",null));
                assertEquals("java.lang.Exception",XMLFormat.get(SOAPHelper.getEnvelope(res),
                        "Body.Fault.detail.class",null));
                assertNotNull(XMLFormat.get(SOAPHelper.getEnvelope(res),
                        "Body.Fault.detail.requestId",null));

            }
        }));
    }

}
