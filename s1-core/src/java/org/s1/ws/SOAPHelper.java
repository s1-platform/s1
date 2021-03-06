/*
 * Copyright 2014 Grigory Pykhov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.s1.ws;

import org.s1.S1SystemError;
import org.s1.format.xml.XMLFormat;
import org.s1.format.xml.XSDFormatException;
import org.s1.format.xml.XSDValidationException;
import org.s1.misc.Base64;
import org.s1.misc.Base64FormatException;
import org.s1.misc.IOUtils;
import org.s1.objects.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.XMLConstants;
import javax.xml.soap.*;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * SOAP Format helper
 */
public class SOAPHelper {

    private static final Logger LOG = LoggerFactory.getLogger(SOAPHelper.class);

    /**
     * Create message from string
     *
     * @param soap
     * @return
     */
    public static SOAPMessage createSoapFromString(String soap){
        return createSoapFromString(null,soap);
    }

    public static SOAPMessage createSoapFromString(String protocol, String soap){
        if(Objects.isNullOrEmpty(protocol)) {
            if(soap.contains("http://www.w3.org/2003/05/soap-envelope") &&
                    !soap.contains("http://schemas.xmlsoap.org/soap/envelope/")){
                protocol = SOAPConstants.SOAP_1_2_PROTOCOL;
            }else{
                protocol = SOAPConstants.SOAP_1_1_PROTOCOL;
            }
        }
        try{
            MessageFactory messageFactory = MessageFactory.newInstance(protocol);
            return messageFactory.createMessage(null,new ByteArrayInputStream(soap.getBytes("UTF-8")));
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

    /**
     * Get envelope element quite
     *
     * @param m
     * @return
     */
    public static Element getEnvelope(SOAPMessage m){
        try{
            return m.getSOAPPart().getEnvelope();
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

    /**
     * Create message from stream with headers
     *
     * @param headers
     * @param is
     * @return
     */
    public static SOAPMessage createSoapFromStream(Map<String,String> headers, InputStream is){
        return createSoapFromStream(null,headers,is);
    }

    public static SOAPMessage createSoapFromStream(String protocol, Map<String,String> headers, InputStream is){
        if(headers==null)
            headers = Objects.newHashMap();
        MimeHeaders sh = new MimeHeaders();
        for(String k:headers.keySet()){
            sh.addHeader(k,headers.get(k));
        }
        String p = protocol;
        if(Objects.isNullOrEmpty(protocol))
            p = SOAPConstants.SOAP_1_1_PROTOCOL;
        try{
            try {
                MessageFactory messageFactory = MessageFactory.newInstance(p);
                return messageFactory.createMessage(sh, is);
            }catch(SOAPException e){
                if("SOAPVersionMismatchException".equals(e.getClass().getSimpleName()) && Objects.isNullOrEmpty(protocol)){
                    MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
                    return messageFactory.createMessage(sh, is);
                }else{
                    throw e;
                }
            }
        }catch (Exception e){

            throw S1SystemError.wrap(e);
        }
    }

    /**
     * Validate message on wsdl
     *
     * @param wsdl
     * @param msg
     * @throws XSDFormatException
     * @throws XSDValidationException
     */
    public static void validateMessage(String basePath, Document wsdl, SOAPMessage msg) throws XSDFormatException, XSDValidationException{
        LOG.debug("Validating message on WSDL");

        //get schema
        //Element schemaNode = (Element)wsdl.getElementsByTagNameNS(XMLConstants"http://www.w3.org/2001/XMLSchema","schema").item(0);
        NodeList schemaNodes = wsdl.getElementsByTagNameNS(
                XMLConstants.W3C_XML_SCHEMA_NS_URI, "schema");

        int nrSchemas = schemaNodes.getLength();

        Source[] schemas = new Source[nrSchemas];

        for (int i = 0; i < nrSchemas; i++) {
            schemas[i] = new DOMSource(schemaNodes.item(i));
        }

        //get body
        Element body = null;
        try {
            body = msg.getSOAPBody();
        } catch (SOAPException e) {
            throw S1SystemError.wrap(e);
        }

        if(msg.getAttachments().hasNext()){
            //copy
            body = (Element)body.cloneNode(true);
            //remove xop:Includes
            NodeList nl = body.getElementsByTagNameNS("http://www.w3.org/2004/08/xop/include","Include");
            for(int i=0;i<nl.getLength();i++){
                Node n = nl.item(i);
                n.getParentNode().removeChild(n);
            }
        }

        //validate Body children
        for(Element el:XMLFormat.getChildElementList(body,null,null)){
            XMLFormat.validate(basePath, schemas, el);
        }
    }

    /**
     * Send message
     *
     * @param endpoint service address
     * @param data
     * @return
     */
    public static SOAPMessage send(String endpoint, SOAPMessage data) throws SOAPException{
        SOAPConnection soapConnection = null;
        try{
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            soapConnection = soapConnectionFactory.createConnection();

            SOAPMessage soapResponse = soapConnection.call(data, endpoint);

            if(LOG.isTraceEnabled()){
                LOG.trace("Sending soap to endpoint succeed\n" +
                        "                Endpoint: "+endpoint+"\n" +
                        "                SOAP: \n"+(data!=null?SOAPHelper.toString(data):null)+"\n" +
                        "                SOAP Response: \n"+(soapResponse!=null?SOAPHelper.toString(soapResponse):null));
            }else if(LOG.isDebugEnabled()){
                LOG.trace("Sending soap to endpoint succeed\n" +
                        "                        Endpoint: "+endpoint);
            }

            return soapResponse;
        } catch (SOAPException e) {
            if(LOG.isTraceEnabled()){
                LOG.trace("Sending soap to endpoint error: "+e.getClass().getName()+": "+e.getMessage()+"\n" +
                        "                Endpoint: "+endpoint+"\n" +
                        "                SOAP: \n"+(data!=null?SOAPHelper.toString(data):null)+"",e);
            }else if(LOG.isDebugEnabled()){
                LOG.debug("Sending soap to endpoint error: "+e.getClass().getName()+": "+e.getMessage()+"\n" +
                        "                Endpoint: "+endpoint,e);
            }
            throw e;
        } finally {
            try{
                if(soapConnection!=null)
                    soapConnection.close();
            }catch (Throwable e){}
        }
    }

    /**
     * Read binary data from element (base64 or xop:Include)
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

    public static DataHandler readDataHandler(SOAPMessage msg, Element el){
        Element include = XMLFormat.getFirstChildElement(el,"Include","http://www.w3.org/2004/08/xop/include");
        DataHandler is = null;
        if(include!=null){
            //LOG.debug("Reading XOP file")
            String id = "<"+include.getAttribute("href").substring("cid:".length())+">";
            Iterator<AttachmentPart> it = msg.getAttachments();
            while(it.hasNext()){
                AttachmentPart att = it.next();
                if(id.equals(att.getContentId())){
                    try {
                        is = att.getDataHandler();
                    } catch (SOAPException e) {
                        throw S1SystemError.wrap(e);
                    }
                    break;
                }
            }
        }else{
            String b = el.getTextContent();
            try{
                final byte [] data = Base64.decode(b);
                is = new DataHandler(new DataSource(){
                    private InputStream is = new ByteArrayInputStream(data);
                    @Override
                    public InputStream getInputStream() throws IOException {
                        return is;
                    }

                    @Override
                    public OutputStream getOutputStream() throws IOException {
                        return null;
                    }

                    @Override
                    public String getContentType() {
                        return "application/octet-stream";
                    }

                    @Override
                    public String getName() {
                        return null;
                    }
                });
            } catch (Base64FormatException e) {
                throw S1SystemError.wrap(e);
            }
        }
        if(LOG.isDebugEnabled())
            LOG.debug("Reading DataHandler from SOAP, contentType: "+(is==null?-1:is.getDataSource().getContentType()));
        return is;
    }

    /**
     * Write binary data to message as base64 or xop:Include depending on mtom parameter.
     *
     * @param mtom
     * @param msg
     * @param el
     * @param dh
     */
    public static void writeDataHandler(boolean mtom, SOAPMessage msg, Element el, DataHandler dh){
        LOG.debug("Writing DataHandler, mtom: "+mtom+", contentType: "+dh.getContentType());
        if(mtom){
            Element inc = el.getOwnerDocument().createElementNS("http://www.w3.org/2004/08/xop/include","Include");
            el.appendChild(inc);
            String id = UUID.randomUUID().toString()+"@s1-platform.org";
            AttachmentPart ap = msg.createAttachmentPart();
            ap.setDataHandler(dh);
            ap.setContentId("<"+id+">");
            msg.addAttachmentPart(ap);
            inc.setAttribute("href","cid:"+id);
            //LOG.debug("File contentId: "+id);
        }else{
            try {
                el.setTextContent(Base64.encode(IOUtils.toBytes(dh.getInputStream())));
            } catch (IOException e) {
                throw S1SystemError.wrap(e);
            }
        }
    }

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
     * Convert message envelope xml to string
     *
     * @param msg
     * @return
     */
    public static String toString(SOAPMessage msg){
        if(msg==null)
            return null;
        return XMLFormat.toString(msg.getSOAPPart().getDocumentElement());
    }

    /**
     * Write message with attachments to stream
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
