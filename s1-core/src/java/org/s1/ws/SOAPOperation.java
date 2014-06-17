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
import org.s1.format.xml.XMLFormatException;
import org.s1.misc.IOUtils;
import org.s1.objects.BadDataException;
import org.s1.objects.Objects;
import org.s1.weboperation.WebOperation;
import org.slf4j.MDC;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.soap.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Map;

/**
 * Base class for your Web Services
 */
public abstract class SOAPOperation extends WebOperation<SOAPMessage,SOAPMessage> {

    @Override
    protected SOAPMessage parseInput(HttpServletRequest request) throws Exception {
        if(!request.getMethod().equalsIgnoreCase("post")){
            return null;
        }

        Map<String,String> headers = Objects.newHashMap();
        Enumeration<String> he = request.getHeaderNames();
        while(he.hasMoreElements()){
            String h = he.nextElement();
            headers.put(h,request.getHeader(h));
        }
        SOAPMessage msg = SOAPHelper.createSoapFromStream(getProtocol(),headers,request.getInputStream());
        return msg;
    }

    @Override
    protected void formatOutput(SOAPMessage out, boolean error, HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setCharacterEncoding("UTF-8");
        SOAPMessage msg = out;

        Enumeration<String> he = request.getHeaderNames();
        while(he.hasMoreElements()){
            String h = he.nextElement();
            msg.getMimeHeaders().setHeader(h,request.getHeader(h));
        }

        if(msg.getAttachments().hasNext()){
            //has attachments
            msg.getSOAPPart().setContentId("rootpart@s1-platform.org");
        }

        //write content
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        msg.writeTo(bos);

        String ct = msg.getMimeHeaders().getHeader("Content-Type")[0];
        response.setContentType(ct);

        OutputStream os = response.getOutputStream();
        os.write(bos.toByteArray());
        response.flushBuffer();
    }

    @Override
    protected SOAPMessage transformError(Throwable e, HttpServletRequest request, HttpServletResponse response) {
        try{
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage msg = factory.createMessage();
            SOAPFault f = msg.getSOAPBody().addFault();
            f.setFaultCode("Server");
            f.setFaultString(e.getClass().getName() + ": " + e.getMessage());
            Detail d = f.addDetail();
            d.addDetailEntry(QName.valueOf("class")).addTextNode(e.getClass().getName());
            String m = e.getMessage();
            if(m==null)
                m = "";
            d.addDetailEntry(QName.valueOf("message")).addTextNode(m);
            d.addDetailEntry(QName.valueOf("requestId")).addTextNode(MDC.get("requestId"));
            return msg;
        }catch (Exception ex){
            throw S1SystemError.wrap(ex);
        }
    }

    /**
     * Implement business logic here
     *
     * @param action
     * @param request
     * @return
     * @throws Exception
     */
    protected abstract SOAPMessage processSOAP(String method, String action, SOAPMessage request) throws Exception;

    @Override
    protected String inToString(SOAPMessage params) {
        return SOAPHelper.toString(params);
    }

    @Override
    protected String outToString(SOAPMessage out) {
        return SOAPHelper.toString(out);
    }

    /**
     * Get current service address
     *
     * @param request
     * @return
     */
    protected String getAddress(HttpServletRequest request){
        String path = request.getScheme()+"://"+request.getServerName();
        //port
        if(request.getScheme().equals("http")&&request.getServerPort()!=80)
            path+=":"+request.getServerPort();
        if(request.getScheme().equals("https")&&request.getServerPort()!=443)
            path+=":"+request.getServerPort();
        path+=request.getRequestURI();
        return path;
    }

    /*protected boolean shouldValidateInput(String service){
        return true;
    }

    protected boolean shouldValidateOutput(String service){
        return true;
    }*/

    protected String getProtocol(){
        return SOAPConstants.SOAP_1_1_PROTOCOL;
    }

    /**
     * Get resource (WSDL or XSD)
     *
     * @param service
     * @param address service address - will replace ${address} in resource
     * @param resource path to resource relative to /s1ws/ java package
     * @return
     */
    protected abstract Document getResource(String service, String address, String resource) throws Exception;

    /**
     * Get SOAPAction, try to get it from first body child name
     *
     * @param service
     * @param msg
     * @param request
     * @return
     */
    protected String getAction(String service, SOAPMessage msg, HttpServletRequest request){
        if(!request.getMethod().equalsIgnoreCase("post")){
            return null;
        }

        String a = null;
        Element action = null;
        try {
            action = XMLFormat.getFirstChildElement(msg.getSOAPBody(), null, null);
        } catch (SOAPException e) {
            throw S1SystemError.wrap(e);
        }
        if(action!=null)
            a = action.getLocalName();
        return a;
    }

    @Override
    protected SOAPMessage process(String service, SOAPMessage msg, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String address = getAddress(request);

        if("wsdl".equals(request.getQueryString())){
            String wsdl = XMLFormat.toString(getResource(service,address,null).getDocumentElement());
            response.setContentType("text/xml");
            response.setCharacterEncoding("UTF-8");
            OutputStream os = response.getOutputStream();
            os.write(wsdl.getBytes(Charset.forName("UTF-8")));
            return null;
        }else if(!Objects.isNullOrEmpty(request.getQueryString()) && request.getParameter("resource")!=null){
            String resource = request.getParameter("resource");
            String res = XMLFormat.toString(getResource(service,address,resource).getDocumentElement());
            response.setContentType("text/xml");
            response.setCharacterEncoding("UTF-8");
            OutputStream os = response.getOutputStream();
            os.write(res.getBytes(Charset.forName("UTF-8")));
            return null;
        }else{
            if(msg==null)
                throw new BadDataException("Input SOAP message is null");
            //validate
            /*if(shouldValidateInput(service)){
                SOAPHelper.validateMessage(address,getResource(service,address,null),msg);
            }*/

            String action = getAction(service,msg,request);
            SOAPMessage out = processSOAP(service,action,msg);

            //validate
            /*if(shouldValidateOutput(service)){
                SOAPHelper.validateMessage(address,getResource(service,address,null),out);
            }*/

            return out;
        }
    }
}
