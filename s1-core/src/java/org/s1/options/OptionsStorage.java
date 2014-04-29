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

package org.s1.options;

import org.s1.format.json.JSONFormat;
import org.s1.format.json.JSONFormatException;
import org.s1.format.xml.XMLFormat;
import org.s1.format.xml.XMLFormatException;
import org.s1.misc.IOUtils;
import org.s1.misc.protocols.Init;
import org.s1.objects.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Default options storage implementation
 */
public class OptionsStorage {
    static {
        Init.init();
    }

    private static final Logger LOG = LoggerFactory.getLogger(OptionsStorage.class);

    private String configHome;

    public OptionsStorage(){
        configHome = Options.getParameter(CONFIG_HOME);
    }

    /**
     *
     * @param cls
     * @param path
     * @param <T>
     * @return
     */
    public <T> T getSystem(Class<T> cls, String path){
        return get(cls,"",path);
    }

    /**
     *
     * @param cls
     * @param path
     * @param def
     * @param <T>
     * @return
     */
    public <T> T getSystem(Class<T> cls, String path, T def){
        return get(cls,"",path,def);
    }

    /**
     *
     * @param path
     * @param <T>
     * @return
     */
    public <T> T getSystem(String path){
        return get("",path);
    }

    /**
     *
     * @param path
     * @param def
     * @param <T>
     * @return
     */
    public <T> T getSystem(String path, T def){
        return get("",path,def);
    }

    /**
     *
     * @param cls
     * @param name
     * @param path
     * @param <T>
     * @return
     */
    public <T> T get(Class<T> cls, String name, String path){
        return get(cls,name,path,null);
    }

    /**
     *
     * @param cls
     * @param name
     * @param path
     * @param def
     * @param <T>
     * @return
     */
    public <T> T get(Class<T> cls, String name, String path, T def){
        return Objects.cast(get(name,path,def),cls);
    }

    /**
     *
     * @param name
     * @param path
     * @param <T>
     * @return
     */
    public <T> T get(String name, String path){
        return get(name,path,null);
    }

    /**
     *
     * @param name
     * @return
     */
    public <T> T get(String name, String path, T def){
        if(Objects.isNullOrEmpty(name))
            name = SYSTEM_PROPERTIES;
        return Objects.get(getMap(name),path,def);
    }

    public static final String SYSTEM_PROPERTIES = "System";
    public static final String CONFIG_HOME = "ConfigHome";

    /**
     *
     * @param name
     * @return
     */
    public Map<String,Object> getMap(String name){
        Map<String,Object> r = Objects.newHashMap();
        String cfgText = readConfigToString(name+".cfg");
        if(!Objects.isNullOrEmpty(cfgText)){
            try {
                r = parseToMap(cfgText);
            } catch (Exception e) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Cannot parse properties: "+e.getMessage());
                e.printStackTrace();
            }
        }
        if(LOG.isTraceEnabled())
            LOG.trace("Read options properties "+name+": "+r);
        else if(LOG.isDebugEnabled())
            LOG.debug("Read options properties "+name);
        return r;
    }

    public final String TYPE_NS = "http://s1-platform.org/config/types/";

    /**
     *
     * @param config
     * @return
     * @throws JSONFormatException
     * @throws XMLFormatException
     */
    public Map<String,Object> parseToMap(String config) throws JSONFormatException, XMLFormatException{
        if(Objects.isNullOrEmpty(config))
            return Objects.newHashMap();
        config = config.trim();
        if(config.startsWith("<")){
            //xml
            Document doc = XMLFormat.fromString(config);
            return (Map<String,Object>) parseElementToObject(doc.getDocumentElement());
        }else if(config.startsWith("{")){
            return Objects.fromWire(JSONFormat.evalJSON(config));
        }
        return Objects.newHashMap();
    }

    /**
     *
     * @param m
     * @return
     */
    public String formatMap(Map<String,Object> m){
        String s = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!--\n" +
                "~ Copyright 2013-${new Date().format(\"yyyy\")} S1 Platform\n" +
                "~ Created at: ${new Date().format(\"yyyy-MM-dd HH:mm:ss\")}\n" +
                "-->\n" +
                "\n" +
                "<map:cfg\n" +
                "  xmlns:bi=\""+TYPE_NS+"BigInteger\"\n" +
                "  xmlns:bd=\""+TYPE_NS+"BigDecimal\"\n" +
                "  xmlns:i=\""+TYPE_NS+"Integer\"\n" +
                "  xmlns:l=\""+TYPE_NS+"Long\"\n" +
                "  xmlns:f=\""+TYPE_NS+"Float\"\n" +
                "  xmlns:d=\""+TYPE_NS+"Double\"\n" +
                "  xmlns:dt=\""+TYPE_NS+"Date\"\n" +
                "  xmlns:b=\""+TYPE_NS+"Boolean\"\n" +
                "  xmlns:map=\""+TYPE_NS+"Map\"\n" +
                "  xmlns:list=\""+TYPE_NS+"List\">\n";
        //iterate map
        s+=formatObject("  ",m);
        s+="\n</map:cfg>";
        return s;
    }

    /**
     * Do not forget to close InputStream
     *
     * @param name
     * @return
     */
    public InputStream openConfig(String name) {
        InputStream is = null;
        String configPath = configHome + "/" + name;
        if(!configPath.matches("^[a-z]+:/.+$")) {
            File file = new File(configPath);
            if (file.exists() && file.isFile()) {
                try {
                    is = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    LOG.debug("Config " + name + " (" + configPath + ") not found");
                }
            }
        }else {
            try {
                URLConnection c = new URL(configPath).openConnection();
                is = c.getInputStream();
            } catch (Throwable e) {
                LOG.debug("Config " + name + " (" + configPath + ") not found", e);
            }
        }
        return is;
    }

    /**
     *
     * @param name
     * @return
     */
    public String readConfigToString(String name){
        InputStream is = null;
        try{
            is = openConfig(name);
            if(is==null)
                return null;
            return IOUtils.toString(is, "UTF-8");
        }finally {
            IOUtils.closeQuietly(is);
        }
    }

    /**
     *
     * @param el
     * @return
     */
    private Object parseElementToObject(Element el){
        String ns = el.getNamespaceURI();

        String type = "";
        if(!Objects.isNullOrEmpty(ns) && ns.startsWith(TYPE_NS)){
            type = ns.substring(TYPE_NS.length());
        }
        if(Objects.isNullOrEmpty(type))
            type="String";

        if("Map".equals(type)){
            Map<String,Object> m = Objects.newHashMap();
            NamedNodeMap attrs = el.getAttributes();
            for(int i=0;i<attrs.getLength();i++){
                Node attr = attrs.item(i);
                String attr_ns = attr.getNamespaceURI();
                if(!"http://www.w3.org/2000/xmlns/".equals(attr_ns)){
                    String o = attr.getNodeValue();
                    String attr_type = "";
                    if(!Objects.isNullOrEmpty(attr_ns) && attr_ns.startsWith(TYPE_NS)){
                        attr_type = attr_ns.substring(TYPE_NS.length());
                    }
                    if(Objects.isNullOrEmpty(attr_type))
                        attr_type="String";
                    m.put(attr.getLocalName(),Objects.cast(o,attr_type));
                }
            }
            //children
            for(Element it:XMLFormat.getChildElementList(el,null,null)){
                m.put(it.getLocalName(), parseElementToObject(it));
            }
            return m;
        }else if("List".equals(type)){
            List<Object> lst = Objects.newArrayList();
            //children
            for(Element it:XMLFormat.getChildElementList(el,null,null)){
                lst.add(parseElementToObject(it));
            }
            return lst;
        }else{
            Object o = el.getTextContent();
            if("Date".equals(type)){
                try{
                    o = Objects.parseDate((String)o,"yyyy-MM-dd HH:mm:ss.SSS");
                }catch (Exception e){
                    o = Objects.fromWire(Objects.newHashMap(String.class,Object.class,"date",o)).get("date");
                }
            }
            o = Objects.cast(o,type);
            return o;
        }
    }

    /**
     *
     * @param tab
     * @param name
     * @param o
     * @return
     */
    private String wrapObject(String tab, String name, Object o){
        String s = "\n"+tab+"<";
        String t = "";
        String end = "";
        if(o instanceof Map){
            t="map:";
            end = "\n"+tab;
        }else if(o instanceof List){
            t="list:";
            end = "\n"+tab;
        }else if(o instanceof Date){
            t="dt:";
        }else if(o instanceof Boolean){
            t="b:";
        }else if(o instanceof Integer){
            t="i:";
        }else if(o instanceof Long){
            t="l:";
        }else if(o instanceof Float){
            t="f:";
        }else if(o instanceof Double){
            t="d:";
        }else if(o instanceof BigDecimal){
            t="bd:";
        }else if(o instanceof BigInteger){
            t="bi:";
        }else if(o instanceof String){

        }
        s+=t+name+">";
        s+=formatObject(tab,o);
        s+=end+"</"+t+name+">";
        return s;
    }

    /**
     *
     * @param tab
     * @param o
     * @return
     */
    private String formatObject(String tab, Object o){
        String s = "";
        if(o instanceof Map){
            for(Map.Entry<String,Object> it:((Map<String,Object>) o).entrySet()) {
                s+=wrapObject(tab+"  ",it.getKey(),it.getValue());
            }
        }else if(o instanceof List){
            for(Object it:(List)o) {
                s+=wrapObject(tab+"  ","e",it);
            }
        }else if(o instanceof Date){
            s = escapeXml(Objects.formatDate((Date) o, "yyyy-MM-dd HH:mm:ss.SSS"));
        }else if(o instanceof Boolean){
            s = escapeXml("" + o);
        }else if(o instanceof Integer){
            s = escapeXml("" + o);
        }else if(o instanceof Long){
            s = escapeXml("" + o);
        }else if(o instanceof Float){
            s = escapeXml("" + o);
        }else if(o instanceof Double){
            s = escapeXml("" + o);
        }else if(o instanceof BigDecimal){
            s = escapeXml("" + o);
        }else if(o instanceof BigInteger){
            s = escapeXml("" + o);
        }else if(o instanceof String){
            s = escapeXml((String) o);
        }
        return s;
    }

    /**
     *
     * @param s
     * @return
     */
    private String escapeXml(String s){
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }

}
