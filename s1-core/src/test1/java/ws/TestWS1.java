package ws;

import org.s1.format.xml.XMLFormat;
import org.s1.ws.SOAPHelper;
import org.s1.ws.SOAPOperation;

import javax.xml.soap.SOAPMessage;

/**
 * s1v2
 * User: GPykhov
 * Date: 25.01.14
 * Time: 15:34
 */
public class TestWS1 extends SOAPOperation {

    @Override
    protected SOAPMessage processSOAP(String action, SOAPMessage request) throws Exception {
        if(!"SignRequest".equals(action))
            throw new Exception("action");
        String r = new String(SOAPHelper.readFile(request,XMLFormat.getElement(SOAPHelper.getEnvelope(request), "Body.SignRequest.data", null)));
        String soap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "                <SOAP-ENV:Header> </SOAP-ENV:Header>\n" +
                "                <SOAP-ENV:Body>\n" +
                "                <cry:SignResponse xmlns:cry=\"http://example.com/\">\n" +
                "                <data></data>\n" +
                "                </cry:SignResponse>\n" +
                "                </SOAP-ENV:Body>\n" +
                "                </SOAP-ENV:Envelope>";

        r+="1";
        SOAPMessage msg = SOAPHelper.createSoapFromString(soap);
        SOAPHelper.writeFile(request.getAttachments().hasNext(), msg, XMLFormat.getElement(SOAPHelper.getEnvelope(msg), "Body.SignResponse.data", null), r.getBytes());
        return msg;
    }
}
