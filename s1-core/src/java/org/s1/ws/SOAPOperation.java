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
        if("wsdl".equals(request.getQueryString())
                || (!Objects.isNullOrEmpty(request.getQueryString()) && request.getQueryString().startsWith("import"))){
            return null;
        }

        Map<String,String> headers = Objects.newHashMap();
        Enumeration<String> he = request.getHeaderNames();
        while(he.hasMoreElements()){
            String h = he.nextElement();
            headers.put(h,request.getHeader(h));
        }
        SOAPMessage msg = SOAPHelper.createSoapFromStream(headers,request.getInputStream());

        //validate
        if(Objects.get(config,"ValidateInput",false)){
            String path = getAddress(request);
            String wsdlPath = Objects.get(config,"WSDL");
            SOAPHelper.validateMessage(getResource(wsdlPath,path),msg);
        }

        return msg;
    }

    /**
     * Get resource (WSDL or XSD)
     *
     * @param path path to resource relative to /s1ws/ java package
     * @param address service address - will replace ${address} in resource
     * @return
     */
    protected Document getResource(String path, String address) throws XMLFormatException{
        if(path.startsWith("/"))
            path = path.substring(1);
        String res = IOUtils.toString(this.getClass().getResourceAsStream("/s1ws/"+path),"UTF-8");
        res = res.replace("${address}",address);
        return XMLFormat.fromString(res);
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

        //validate
        if(Objects.get(config,"ValidateOutput",false)){
            String path = getAddress(request);
            String wsdlPath = Objects.get(config,"WSDL");
            SOAPHelper.validateMessage(getResource(wsdlPath,path),msg);
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
     * Get SOAPAction, try to get it from first body child name
     *
     * @param method
     * @param msg
     * @param request
     * @return
     */
    protected String getAction(String method, SOAPMessage msg, HttpServletRequest request){
        if("wsdl".equals(request.getQueryString())
                || (!Objects.isNullOrEmpty(request.getQueryString()) && request.getQueryString().startsWith("import"))){
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

    /**
     * Implement business logic here
     *
     * @param action
     * @param request
     * @return
     * @throws Exception
     */
    protected abstract SOAPMessage processSOAP(String action, SOAPMessage request) throws Exception;

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
        String path = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+request.getRequestURI();
        return path;
    }

    @Override
    protected SOAPMessage process(String method, SOAPMessage msg, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if("wsdl".equals(request.getQueryString())){
            String wsdlPath = Objects.get(config,"wsdl");
            String path = getAddress(request);
            String wsdl = XMLFormat.toString(getResource(wsdlPath, path).getDocumentElement());
            response.setContentType("text/xml");
            response.setCharacterEncoding("UTF-8");
            OutputStream os = response.getOutputStream();
            os.write(wsdl.getBytes(Charset.forName("UTF-8")));
            return null;
        }else if(!Objects.isNullOrEmpty(request.getQueryString()) && request.getQueryString().startsWith("import")){
            String p = request.getParameter("import");
            String path = getAddress(request);
            String res = XMLFormat.toString(getResource(p,path).getDocumentElement());
            response.setContentType("text/xml");
            response.setCharacterEncoding("UTF-8");
            OutputStream os = response.getOutputStream();
            os.write(res.getBytes(Charset.forName("UTF-8")));
            return null;
        }else{
            String action = getAction(method,msg,request);
            SOAPMessage out = processSOAP(action,msg);
            return out;
        }
    }
}
