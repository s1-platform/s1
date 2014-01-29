package org.s1.ws;

import org.s1.S1SystemError;
import org.s1.format.xml.XMLFormat;
import org.s1.format.xml.XSDFormatException;
import org.s1.format.xml.XSDValidationException;
import org.s1.misc.Base64;
import org.s1.misc.Base64FormatException;
import org.s1.objects.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.soap.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * SOAP Format helper
 */
public class SOAPHelper {

    private static final Logger LOG = LoggerFactory.getLogger(SOAPHelper.class);

    public static SOAPMessage createSoapFromString(String soap){
        try{
            MessageFactory messageFactory = MessageFactory.newInstance();
            return messageFactory.createMessage(null,new ByteArrayInputStream(soap.getBytes("UTF-8")));
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

    public static Element getEnvelope(SOAPMessage m){
        try{
            return m.getSOAPPart().getEnvelope();
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

    /**
     *
     * @param headers
     * @param is
     * @return
     */
    public static SOAPMessage createSoapFromStream(Map<String,String> headers, InputStream is){
        if(headers==null)
            headers = Objects.newHashMap();
        MimeHeaders sh = new MimeHeaders();
        for(String k:headers.keySet()){
            sh.addHeader(k,headers.get(k));
        }
        try{
            MessageFactory messageFactory = MessageFactory.newInstance();
            return messageFactory.createMessage(sh,is);
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

    public static void validateMessage(Document wsdl, SOAPMessage msg) throws XSDFormatException, XSDValidationException{
        LOG.debug("Validating message on WSDL");

        //replace base64Binary with xsd:any
        /*NodeList elementNodeSet = wsdl.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema","element");
        for(int i=0;i<elementNodeSet.getLength();i++){
            if(elementNodeSet.item(i) instanceof Element){
                Element element = (Element)elementNodeSet.item(i);
                if(element.getAttribute("type").endsWith("base64Binary")){
                    Element ct = wsdl.createElementNS("http://www.w3.org/2001/XMLSchema","complexType");
                    Element seq = wsdl.createElementNS("http://www.w3.org/2001/XMLSchema","sequence");
                    Element any = wsdl.createElementNS("http://www.w3.org/2001/XMLSchema","any");
                    ct.appendChild(seq);
                    ct.setAttribute("mixed","true");
                    seq.appendChild(any);
                    seq.setAttribute("minOccurs","0");
                    seq.setAttribute("maxOccurs","unbounded");
                    element.appendChild(ct);
                    element.removeAttribute("type");
                    any.setAttribute("processContents","skip");
                }

            }
        }*/
        if(msg.getAttachments().hasNext())
            return;

        //get schema
        Element schemaNode = (Element)wsdl.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema","schema").item(0);
        Element el = null;
        try{
            el = XMLFormat.getFirstChildElement(msg.getSOAPBody(), null, null);
        }catch (SOAPException e){
            throw S1SystemError.wrap(e);
        }
        XMLFormat.validate(schemaNode, el);
    }

    /**
     *
     * @param endpoint
     * @param data
     * @return
     */
    public static SOAPMessage send(String endpoint, SOAPMessage data) throws SOAPException{
        try{
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection soapConnection = soapConnectionFactory.createConnection();

            SOAPMessage soapResponse = soapConnection.call(data, endpoint);

            if(LOG.isTraceEnabled()){
                LOG.trace("Sending soap to endpoint succeed\n" +
                        "                        Endpoint: ${endpoint}\n" +
                        "                SOAP: \\n${data?SOAPHelper.toString(data):null}\n" +
                        "                SOAP Response: \\n${data?SOAPHelper.toString(soapResponse):null}");
            }else if(LOG.isDebugEnabled()){
                LOG.trace("Sending soap to endpoint succeed\n" +
                        "                        Endpoint: ${endpoint}");
            }

            return soapResponse;
        } catch (SOAPException e) {
            if(LOG.isTraceEnabled()){
                LOG.trace("Sending soap to endpoint error: ${e.getClass().getName()}: ${e.getMessage()}\n" +
                        "                        Endpoint: ${endpoint}\n" +
                        "                SOAP: \\n${data?SOAPHelper.toString(data):null}",e);
            }else if(LOG.isDebugEnabled()){
                LOG.debug("Sending soap to endpoint error: ${e.getClass().getName()}: ${e.getMessage()}\n" +
                        "                        Endpoint: ${endpoint}",e);
            }
            throw e;
        }
    }

    /**
     *
     * @param msg
     * @param el
     * @return
     */
    public static byte [] readFile(SOAPMessage msg, Element el){
        Element include = XMLFormat.getFirstChildElement(el,"Include","http://www.w3.org/2004/08/xop/include");
        byte [] data  = null;
        if(include!=null){
            //LOG.debug("Reading XOP file")
            String id = "<"+include.getAttribute("href").substring("cid:".length())+">";
            Iterator<AttachmentPart> it = msg.getAttachments();
            while(it.hasNext()){
                AttachmentPart att = it.next();
                if(id.equals(att.getContentId())){
                    try {
                        data = att.getRawContentBytes();
                    } catch (SOAPException e) {
                        throw S1SystemError.wrap(e);
                    }
                    break;
                }
            }
        }else{
            //LOG.debug("Reading base64 file")
            String b = el.getTextContent();
            try{
                data = Base64.decode(b);
            } catch (Base64FormatException e) {
                throw S1SystemError.wrap(e);
            }
        }
        if(LOG.isDebugEnabled())
            LOG.debug("Reading file from SOAP, length: "+(data==null?-1:data.length));
        return data;
    }

    /**
     *
     * @param mtom
     * @param msg
     * @param el
     * @param b
     */
    public static void writeFile(boolean mtom, SOAPMessage msg, Element el, byte [] b){
        LOG.debug("Writing file, mtom: "+mtom+", size: "+b.length);
        if(mtom){
            Element inc = el.getOwnerDocument().createElementNS("http://www.w3.org/2004/08/xop/include","Include");
            el.appendChild(inc);
            String id = UUID.randomUUID().toString()+"@s1-platform.org";
            AttachmentPart ap = msg.createAttachmentPart();
            try {
                ap.setRawContentBytes(b,0,b.length,"application/octet-stream");
            } catch (SOAPException e) {
                throw S1SystemError.wrap(e);
            }
            ap.setContentId("<"+id+">");
            msg.addAttachmentPart(ap);
            inc.setAttribute("href","cid:"+id);
            //LOG.debug("File contentId: "+id);
        }else{
            el.setTextContent(Base64.encode(b));
        }
    }

    /**
     *
     * @param msg
     * @return
     */
    public static String toString(SOAPMessage msg){
        if(msg==null)
            return null;
        //ByteArrayOutputStream os = new ByteArrayOutputStream();
        //msg.writeTo(os);
        //return new String(os.toByteArray(),"UTF-8");
        return XMLFormat.toString(msg.getSOAPPart().getDocumentElement());
    }

    /**
     *
     * @param msg
     * @return
     */
    public static InputStream toInputStream(SOAPMessage msg){
        if(msg==null)
            return null;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            msg.writeTo(os);
        } catch (Exception e) {
            throw S1SystemError.wrap(e);
        }
        return new ByteArrayInputStream(os.toByteArray());
    }

}
