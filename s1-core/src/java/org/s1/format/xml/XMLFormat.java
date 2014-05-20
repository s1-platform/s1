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

package org.s1.format.xml;

import org.s1.S1SystemError;
import org.s1.misc.FileUtils;
import org.s1.misc.IOUtils;
import org.s1.objects.ObjectPath;
import org.s1.objects.Objects;
import org.w3c.dom.*;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * XML Format helper
 */
public class XMLFormat {

    /**
     *
     * @param xml
     * @return
     * @throws XMLFormatException
     */
    public static Document fromString(String xml) throws XMLFormatException{
        try{
            ByteArrayInputStream bis = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setCoalescing(true);
            final DocumentBuilder loader = factory.newDocumentBuilder();
            final Document doc = loader.parse(bis);
            return doc;
        }catch (Throwable e){
            throw new XMLFormatException(e.getMessage(),e);
        }
    }

    /**
     *
     * @param doc
     * @return
     */
    public static String toString(Document doc){
        return toString(doc.getDocumentElement());
    }

    /**
     *
     * @param el
     * @return
     */
    public static String toString(Element el){
        try{
            final TransformerFactory tf = TransformerFactory.newInstance();
            final Transformer transformer = tf.newTransformer();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(el),new StreamResult(os));
            return new String(os.toByteArray(),"UTF-8");
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

    /**
     *
     * @param number
     * @param doc
     * @param name
     * @param ns
     * @return
     */
    public static Element getChildElement(int number, Document doc, String name, String ns){
        return getChildElement(number,doc.getDocumentElement(),name,ns);
    }

    /**
     *
     * @param number
     * @param el
     * @param name
     * @param ns
     * @return
     */
    public static Element getChildElement(int number, Element el, String name, String ns){
        List<Element> lst = getChildElementList(el,name,ns);
        if(number<0)
            number = lst.size()-1;

        if(number>=0 && number<lst.size())
            return lst.get(number);
        return null;
    }

    /**
     *
     * @param doc
     * @param name
     * @param ns
     * @return
     */
    public static Element getFirstChildElement(Document doc, String name, String ns){
        return getFirstChildElement(doc.getDocumentElement(), name, ns);
    }

    /**
     *
     * @param el
     * @param name
     * @param ns
     * @return
     */
    public static Element getFirstChildElement(Element el, String name, String ns){
        return getChildElement(0,el,name,ns);
    }

    /**
     *
     * @param doc
     * @param name
     * @param ns
     * @return
     */
    public static Element getLastChildElement(Document doc, String name, String ns){
        return getLastChildElement(doc.getDocumentElement(), name, ns);
    }

    /**
     *
     * @param el
     * @param name
     * @param ns
     * @return
     */
    public static Element getLastChildElement(Element el, String name, String ns){
        return getChildElement(-1,el,name,ns);
    }

    /**
     *
     * @param doc
     * @param name
     * @param ns
     * @return
     */
    public static List<Element> getChildElementList(Document doc, String name, String ns){
        return getChildElementList(doc.getDocumentElement(),name,ns);
    }

    /**
     *
     * @param el
     * @param name
     * @param ns
     * @return
     */
    public static List<Element> getChildElementList(Element el, String name, String ns){
        List<Element> lst = new ArrayList<Element>();
        NodeList nl = el.getChildNodes();
        for(int i=0;i<nl.getLength();i++){
            Node n = nl.item(i);
            if(n instanceof Element){
                Element e = (Element)n;
                if(name!=null){
                    if(name.equals(n.getLocalName())){
                        if(ns!=null){
                            if(ns.equals(n.getNamespaceURI()))
                                lst.add(e);
                        }else
                            lst.add(e);
                    }
                }else{
                    lst.add(e);
                }
            }
        }
        return lst;
    }

    /**
     *
     * @param name
     * @return
     */
    private static int getNumber(String name) {
        if(name.indexOf("[")<name.indexOf("]"))
            return Integer.parseInt(name.substring(name.indexOf("[") + 1, name.indexOf("]")));
        else
            return 0;
    }

    /**
     *
     * @param name
     * @return
     */
    private static String getLocalName(String name) {
        if(name.startsWith("@"))
            name = name.substring(1);
        if(name.contains("["))
            name = name.substring(0,name.indexOf("["));
        if(name.contains(":"))
            name = name.substring(name.indexOf(":")+1);
        return name;
    }

    /**
     *
     * @param name
     * @param ns
     * @return
     */
    private static String getNamespaceURI(String name, Map<String,String> ns) {
        if(name.startsWith("@"))
            name = name.substring(1);
        if(name.contains("["))
            name = name.substring(0,name.indexOf("["));
        if(name.contains(":"))
            name = name.substring(0,name.indexOf(":"));
        else
            name = null;
        if(!Objects.isNullOrEmpty(name))
            name = ns.get(name);
        return name;
    }

    /**
     *
     * @param doc
     * @param path
     * @param namespaces
     * @return
     */
    public static Element getElement(Document doc, String path, Map<String,String> namespaces){
        return getElement(doc.getDocumentElement(),path,namespaces);
    }

    /**
     *
     * @param el
     * @param path
     * @param namespaces
     * @return
     */
    public static Element getElement(Element el, String path, Map<String,String> namespaces){
        if(namespaces==null)
            namespaces = Objects.newHashMap();
        Element ret = null;
        try {
            String[] parts = ObjectPath.tokenizePath(path);
            Element o = el;
            for (int i = 0; i < parts.length; i++) {
                int j = getNumber(parts[i]);
                String name = getLocalName(parts[i]);
                String ns = getNamespaceURI(parts[i],namespaces);
                o = getChildElement(j,o,name,ns);
            }
            if (o != null){
                ret = o;
            }
        } catch (Throwable e) {
        }
        return ret;
    }

    /**
     *
     * @param doc
     * @param path
     * @param namespaces
     * @return
     */
    public static String get(Document doc, String path, Map<String,String> namespaces){
        return get(doc.getDocumentElement(), path, namespaces);
    }

    /**
     *
     * @param el
     * @param path
     * @param namespaces
     * @return
     */
    public static String get(Element el, String path, Map<String,String> namespaces){
        return get(el,path,namespaces,null);
    }

    /**
     *
     * @param doc
     * @param path
     * @param namespaces
     * @param defVal
     * @return
     */
    public static String get(Document doc, String path, Map<String,String> namespaces, String defVal){
        return get(doc.getDocumentElement(),path,namespaces,defVal);
    }

    /**
     *
     * @param el
     * @param path
     * @param namespaces
     * @param defVal
     * @return
     */
    public static String get(Element el, String path, Map<String,String> namespaces, String defVal){
        if(namespaces==null)
            namespaces = Objects.newHashMap();
        String ret = null;
        try {
            String[] parts = ObjectPath.tokenizePath(path);
            for (int i = 0; i < parts.length; i++) {
                int j = getNumber(parts[i]);
                String name = getLocalName(parts[i]);
                String ns = getNamespaceURI(parts[i],namespaces);
                if(parts[i].startsWith("@")){
                    if(ns!=null){
                        ret = el.getAttributeNS(ns,name);
                    }else{
                        ret = el.getAttribute(name);
                    }
                    break;
                }else
                    el = getChildElement(j,el,name,ns);
            }
            if (ret == null && el != null)
                ret = el.getTextContent();
        } catch (Throwable e) {
        }
        if(ret==null)
            ret = defVal;
        return ret;
    }

    /**
     *
     * @param xml
     * @return
     */
    public static Map<String,Object> toMap(Document xml){
        return toMap(xml.getDocumentElement());
    }

    /**
     *
     * @param xml
     * @return
     */
    public static Map<String,Object> toMap(Element xml){
        return toMap(xml,null);
    }

    /**
     *
     * @param xml
     * @param listsPath
     * @return
     */
    public static Map<String,Object> toMap(Document xml, List<String> listsPath){
        return toMap(xml.getDocumentElement(),listsPath);
    }

    /**
     *
     * @param xml
     * @param listsPath
     * @return
     */
    public static Map<String,Object> toMap(Element xml, List<String> listsPath){
        if(listsPath==null)
            listsPath = Objects.newArrayList();
        return convertElementToMap(".", xml, listsPath);
    }

    /**
     *
     * @param path
     * @param el
     * @param listsPath
     * @return
     */
    private static Map<String,Object> convertElementToMap(String path, Element el, List<String> listsPath){
        if(".".equals(path)){
            path = null;
        }else if(path!=null){
            path = path+"."+el.getLocalName().replace(".","\\.");
        }else{
            path = el.getLocalName().replace(".","\\.");
        }

        Map<String,Object> m = Objects.newHashMap();
        for(Element it:XMLFormat.getChildElementList(el,null,null)){
            Object c = null;
            String p = it.getLocalName().replace(".","\\.");
            if(path!=null)
                p = path+"."+p;
            if(XMLFormat.getChildElementList(it,null,null).size()>0 ||
                    it.getAttributes().getLength()>0){
                //map
                c = convertElementToMap(path, it, listsPath);
            }else{
                //value
                c = it.getTextContent();
            }
            //add
            if(m.containsKey(it.getLocalName())){
                if(m.get(it.getLocalName()) instanceof List){
                    ((List) m.get(it.getLocalName())).add(c);
                }else{
                    Object old = m.get(it.getLocalName());
                    m.put(it.getLocalName(),Objects.newArrayList(old,c));
                }
            }else{
                //check xsd if array
                if(listsPath.contains(p)){
                    c = Objects.newArrayList(c);
                }
                m.put(it.getLocalName(),c);
            }
        }
        //attributes
        if(el.getAttributes().getLength()>0){
            //map
            NamedNodeMap attrs = el.getAttributes();
            for(int i=0;i<attrs.getLength();i++){
                if(!"xmlns".equals(attrs.item(i).getPrefix()))
                    m.put("@"+attrs.item(i).getLocalName(),attrs.item(i).getNodeValue());
            }
        }
        return m;
    }

    /**
     *
     * @param xsd
     * @param xml
     * @throws XSDFormatException
     * @throws XSDValidationException
     */
    public static void validate(final String schemaPath, Document xsd, Document xml) throws XSDFormatException,XSDValidationException {
        validate(schemaPath,xsd.getDocumentElement(),xml.getDocumentElement());
    }

    /**
     *
     * @param xsd
     * @param xml
     * @throws XSDFormatException
     * @throws XSDValidationException
     */
    public static void validate(final String schemaPath, Document xsd, Element xml) throws XSDFormatException,XSDValidationException{
        validate(schemaPath,xsd.getDocumentElement(),xml);
    }

    /**
     *
     * @param xsd
     * @param xml
     */
    public static void validate(final String schemaPath, Element xsd, Element xml) throws XSDFormatException,XSDValidationException{
        DOMSource schemaFile = new DOMSource(xsd);
        DOMSource source = new DOMSource(xml,schemaPath);
        DOMResult xmlFile = new DOMResult();

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setResourceResolver(new LSResourceResolver() {
            @Override
            public LSInput resolveResource(final String type, final String namespaceURI, final String publicId, final String systemId, final String baseURI) {
                String d = null;
                String path = systemId;
                if(path.matches("^[a-z]+:.+$")) {
                } else {
                    path = schemaPath+"/"+path;
                }
                InputStream is = null;
                try{
                    is = FileUtils.readResource(path);
                    d = IOUtils.toString(is,"UTF-8");
                } catch (IOException e){
                    throw S1SystemError.wrap(e);
                }finally {
                    IOUtils.closeQuietly(is);
                }
                final String data = d;
                return new LSInput() {
                    @Override
                    public Reader getCharacterStream() {
                        return new StringReader(data);
                    }

                    @Override
                    public void setCharacterStream(Reader characterStream) {
                    }

                    @Override
                    public InputStream getByteStream() {
                        return new ByteArrayInputStream(data.getBytes(Charset.forName("UTF-8")));
                    }

                    @Override
                    public void setByteStream(InputStream byteStream) {
                    }

                    @Override
                    public String getStringData() {
                        return data;
                    }

                    @Override
                    public void setStringData(String stringData) {
                    }

                    @Override
                    public String getSystemId() {
                        return systemId;
                    }

                    @Override
                    public void setSystemId(String systemId) {
                    }

                    @Override
                    public String getPublicId() {
                        return publicId;
                    }

                    @Override
                    public void setPublicId(String publicId) {
                    }

                    @Override
                    public String getBaseURI() {
                        return baseURI;
                    }

                    @Override
                    public void setBaseURI(String baseURI) {
                    }

                    @Override
                    public String getEncoding() {
                        return "UTF-8";
                    }

                    @Override
                    public void setEncoding(String encoding) {
                    }

                    @Override
                    public boolean getCertifiedText() {
                        return false;
                    }

                    @Override
                    public void setCertifiedText(boolean certifiedText) {
                    }
                };
            }
        });

        Validator validator = null;
        try{
            //schemaFactory.setProperty("")
            Schema schema = schemaFactory.newSchema(schemaFile);
            validator = schema.newValidator();
        }catch (Exception e){
            throw new XSDFormatException(e.getMessage(),e);
        }
        try{
            //validate
            validator.validate(source,xmlFile);
        }catch(Throwable e){
            throw new XSDValidationException(e.getMessage(),e);
        }
    }
}
