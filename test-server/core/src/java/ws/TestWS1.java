package ws;

import org.s1.format.xml.XMLFormat;
import org.s1.format.xml.XMLFormatException;
import org.s1.misc.FileUtils;
import org.s1.misc.IOUtils;
import org.s1.objects.Objects;
import org.s1.ws.SOAPHelper;
import org.s1.ws.SOAPOperation;
import org.w3c.dom.Document;

import javax.xml.soap.SOAPMessage;
import java.io.InputStream;

/**
 * s1v2
 * User: GPykhov
 * Date: 25.01.14
 * Time: 15:34
 */
public class TestWS1 extends SOAPOperation {

    @Override
    protected Document getResource(String service, String address, String resource) throws Exception {
        if(Objects.isNullOrEmpty(resource))
            resource = "wsdl.xml";
        InputStream is = null;
        try{
            is = FileUtils.readResource("classpath:/ws/"+resource);
            String s = IOUtils.toString(is,"UTF-8");
            s = s.replaceAll("\\$\\{address\\}",address);
            return XMLFormat.fromString(s);
        }finally {
            IOUtils.closeQuietly(is);
        }
    }

    @Override
    protected SOAPMessage processSOAP(String service, String action, SOAPMessage request) throws Exception {
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
