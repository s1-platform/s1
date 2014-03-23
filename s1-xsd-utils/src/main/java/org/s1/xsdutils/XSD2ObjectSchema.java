package org.s1.xsdutils;

import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.impl.xs.XSElementDecl;
import org.apache.xerces.impl.xs.XSParticleDecl;
import org.apache.xerces.xs.*;
import org.s1.format.xml.XMLFormat;
import org.s1.format.xml.XMLFormatException;
import org.s1.misc.Closure;
import org.s1.objects.ObjectPath;
import org.s1.objects.Objects;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 16.01.14
 * Time: 20:07
 */
public class XSD2ObjectSchema {

    private static XSTypeDefinition getBaseSimpleType(XSTypeDefinition type){
        while(true){
            if("anySimpleType".equals(type.getName()) || "anySimpleType".equals(type.getBaseType().getName())){
                break;
            }
            type = type.getBaseType();
        }
        return type;
    }

    private static String getAnnotation(XSAnnotation a){
        return getAnnotation(a,null);
    }

    private static String getAnnotation(XSAnnotation a, String path){
        if(Objects.isNullOrEmpty(path))
            path = "documentation";
        try {
            return XMLFormat.get(XMLFormat.fromString(a.getAnnotationString()).getDocumentElement(), path, null, "");
        } catch (Throwable e) {
        }
        return null;
    }

    private static Map<String,Object> xstypeToAttrMap(XSTypeDefinition type){
        Map<String,Object> attr = Objects.newHashMap();
        if(type instanceof XSComplexTypeDecl){
            //map or ref
            if(type.getAnonymous()){
                //map
                attr.put("type","Map");
                attr.put("attributes",Objects.newArrayList());
            }else{
                //ref
                attr.put("type","#"+type.getName());
            }
        }else{
            //simple type
            XSTypeDefinition st = getBaseSimpleType(type);
            setAttrWithXSDType(attr,st.getName());
        }
        return attr;
    }

    private static void setAttrWithXSDType(Map<String,Object> attr, String type){
        if(attr.get("hint")==null)
            attr.put("hint",Objects.newHashMap());
        if(Objects.newArrayList("string","anyURI","normalizedString","anySimpleType").contains(type)){
            attr.put("type","String");
        }else if(Objects.newArrayList("int","integer","nonNegativeInteger","nonPositiveInteger","short","unsignedInt","unsignedShort","unsignedByte","byte").contains(type)){
            attr.put("type","Integer");
        }else if(Objects.newArrayList("long","unsignedLong").contains(type)){
            attr.put("type","Long");
        }else if("float".equals(type)){
            attr.put("type","Float");
        }else if("double".equals(type)){
            attr.put("type","Double");
        }else if("decimal".equals(type)){
            attr.put("type","BigDecimal");
        }else if("boolean".equals(type)){
            attr.put("type","Boolean");
        }else if("date".equals(type)){
            attr.put("type","Date");
        }else if("dateTime".equals(type)){
            attr.put("type","Date");
            Objects.set(attr,"hint.type","dateSecond");
        }
    }

    private static final int MAX_LABEL_LENGTH = 100;

    /**
     *
     * @param xsd
     * @return
     */
    public static Map<String,Object> toSchemaMap(Document xsd){
        return toSchemaMap(xsd.getDocumentElement());
    }

    /**
     *
     * @param xsd
     * @return
     */
    public static Map<String,Object> toSchemaMap(Element xsd){
        final List<Map<String,Object>> attributes = Objects.newArrayList();
        final List<Map<String,Object>> types = Objects.newArrayList();
        final Map<String,Object> schema = Objects.newHashMap("attributes", attributes, "types", types);
        //final Map<String,Object> attrs = Objects.newHashMap();
        final List<RawAttr> rawAttributes = Objects.newArrayList();


        //def attrs = [:] as TreeMap;
        XSDIterator.iterateXSD(xsd, new Closure<XSDIterator.XSDIterateBean, Object>() {
            @Override
            public Object call(XSDIterator.XSDIterateBean input) {
                Map<String, Object> attr = null;
                if (input.getObject() instanceof XSParticleDecl) {
                    XSTerm term = input.getParticle().getTerm();
                    if (term instanceof XSElementDecl) {
                        attr = xstypeToAttrMap(((XSElementDecl) term).getTypeDefinition());
                        attr.put("name", term.getName());
                        attr.put("appearance", input.getParticle().getMinOccurs() == 0 ? "normal" : "required");

                        String l = getAnnotation(((XSElementDecl) term).getAnnotation());
                        if (!Objects.isNullOrEmpty(l)) {
                            if (l.length() < MAX_LABEL_LENGTH) {
                                attr.put("label", l);
                            } else {
                                attr.put("label", l.substring(0, MAX_LABEL_LENGTH) + "...");
                                attr.put("description", l);
                            }
                        } else {
                            attr.put("label", attr.get("name"));
                        }
                        if (input.getParticle().getMaxOccursUnbounded() || input.getParticle().getMaxOccurs() > 1) {
                            //list
                            attr = Objects.newHashMap("type", "List", "name", attr.get("name"), "label", attr.get("label"),
                                    "description", attr.get("description"),
                                    "element", attr,
                                    "appearance", "required", "default", Objects.newArrayList());
                            ((Map) attr.get("element")).remove("name");
                            ((Map) attr.get("element")).remove("label");
                            ((Map) attr.get("element")).remove("description");
                            if (input.getParticle().getMinOccurs() > 0)
                                attr.put("min", input.getParticle().getMinOccurs());
                            if (!input.getParticle().getMaxOccursUnbounded())
                                attr.put("max", input.getParticle().getMaxOccurs());
                        } else if (((XSElementDecl) term).getTypeDefinition() instanceof XSComplexTypeDecl) {
                            //map or ref
                            //attr.put("appearance", "required");
                            attr.put("default", Objects.newHashMap());
                        } else {
                            //simple type

                        }
                    } else if (term instanceof XSModelGroup) {
                    }
                } else if (input.getObject() instanceof XSTypeDefinition) {
                    //global type
                    if (input.getTypeDefinition() instanceof XSComplexTypeDecl) {
                        Map<String, Object> tp = xstypeToAttrMap(input.getTypeDefinition());
                        tp.put("name", input.getTypeDefinition().getName());
                        tp.remove("type");
                        tp.put("attributes", Objects.newArrayList());
                        types.add(tp);
                    }
                } else if (input.getObject() instanceof XSElementDecl) {
                    //root elements
                    attr = xstypeToAttrMap(input.getElementDeclaration().getTypeDefinition());
                    attr.put("name", input.getElementDeclaration().getName());
                    attr.put("appearance", "normal");
                    String l = getAnnotation(input.getElementDeclaration().getAnnotation());
                    if (!Objects.isNullOrEmpty(l)) {
                        if (l.length() < MAX_LABEL_LENGTH) {
                            attr.put("label", l);
                        } else {
                            attr.put("label", l.substring(0, MAX_LABEL_LENGTH) + "...");
                            attr.put("description", l);
                        }
                    } else {
                        attr.put("label", attr.get("name"));
                    }
                    if (input.getElementDeclaration().getTypeDefinition() instanceof XSComplexTypeDecl) {
                        //map or ref
                        attr.put("appearance", "required");
                        attr.put("default", Objects.newHashMap());
                    } else {
                        //simple type

                    }
                } if (input.getObject() instanceof XSAttributeUse) {
                    //simple type
                    attr = xstypeToAttrMap(input.getAttributeUse().getAttrDeclaration().getTypeDefinition());
                    attr.put("name", "@" + input.getAttributeUse().getAttrDeclaration().getName());
                    attr.put("appearance", input.getAttributeUse().getRequired() ? "required" : "normal");
                    String l = getAnnotation(input.getAttributeUse().getAttrDeclaration().getAnnotation());
                    if (!Objects.isNullOrEmpty(l)) {
                        if (l.length() < MAX_LABEL_LENGTH) {
                            attr.put("label", l);
                        } else {
                            attr.put("label", l.substring(0, MAX_LABEL_LENGTH) + "...");
                            attr.put("description", l);
                        }
                    } else {
                        attr.put("label", attr.get("name"));
                    }
                }
                //add attr
                if (attr != null) {
                    rawAttributes.add(new RawAttr(input.getPath(), attr));
                }

                return null;
            }
        });

        //create tree
        for(RawAttr it:rawAttributes){
            Map<String,Object> a = getParentAttrFromSchema(schema,it.getPath());
            if(a==null){
                a = schema;
            }
            //println(it.key+":::"+a+":::"+it.value);
            if(!a.containsKey("type") || "Map".equals(a.get("type"))){
                ((List)a.get("attributes")).add(it.getAttribute());
            }else if("List".equals(a.get("type")) && "Map".equals(Objects.get(a, "element.type"))){
                Objects.get(List.class,a,"element.attributes").add(it.getAttribute());
            }
        }

        return schema;
    }

    private static class RawAttr{
        private String path;
        private Map<String,Object> attribute;

        private RawAttr(String path, Map<String, Object> attribute) {
            this.path = path;
            this.attribute = attribute;
        }

        public String getPath() {
            return path;
        }

        public Map<String, Object> getAttribute() {
            return attribute;
        }
    }

    /**
     *
     * @param schema
     * @param path
     * @return
     */
    private static Map<String,Object> getParentAttrFromSchema(Map<String,Object> schema, String path){
        Map<String,Object> a = null;
        boolean isType = false;
        if(path.startsWith("#")){
            path = path.substring(1);
            isType = true;
        }

        final String [] p = ObjectPath.tokenizePath(path);

        /*path.replace("&","&amp;").replace("\\.","&dot;");
        def p = path.split("\\.");
        p.eachWithIndex { String entry, int i ->
            p[i] = p[i].replace("&dot;",".").replace("&amp;","&");
        }*/

        if(isType){
            //type
            a = Objects.find((List<Map<String,Object>>)schema.get("types"),new Closure<Map<String, Object>, Boolean>() {
                @Override
                public Boolean call(Map<String, Object> input) {
                    return input.get("name").equals(p[0]);
                }
            });
        }else{
            //attr
            a = Objects.find((List<Map<String,Object>>)schema.get("attributes"),new Closure<Map<String, Object>, Boolean>() {
                @Override
                public Boolean call(Map<String, Object> input) {
                    return input.get("name").equals(p[0]);
                }
            });
        }

        for(int i=1;i<p.length-1;i++){
            if(a==null){
                break;
            }
            final int index = i;
            if(Objects.isNullOrEmpty(a.get("type")) || "Map".equals(a.get("type"))){
                a = Objects.find((List<Map<String,Object>>)a.get("attributes"),new Closure<Map<String, Object>, Boolean>() {
                    @Override
                    public Boolean call(Map<String, Object> input) {
                        return input.get("name").equals(p[index]);
                    }
                });
            }else if("List".equals(a.get("type")) && "Map".equals(Objects.get(a,"element.type"))){
                a = Objects.find((List<Map<String,Object>>)Objects.get(a,"element.attributes"),new Closure<Map<String, Object>, Boolean>() {
                    @Override
                    public Boolean call(Map<String, Object> input) {
                        return input.get("name").equals(p[index]);
                }
                });
            }else{
                a = null;
            }
        }
        return a;
    }
    
}
