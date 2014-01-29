package org.s1.xsdutils;

import org.apache.xerces.dom.DOMInputImpl;
import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.impl.xs.XSElementDecl;
import org.apache.xerces.impl.xs.XSImplementationImpl;
import org.apache.xerces.impl.xs.XSParticleDecl;
import org.apache.xerces.xs.*;
import org.s1.format.xml.XMLFormat;
import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * s1v2
 * User: GPykhov
 * Date: 16.01.14
 * Time: 20:07
 */
public class XSDIterator {

    /**
     *
     * @param xsd
     * @param closure
     */
    public static void iterateXSD(Document xsd, Closure<XSDIterateBean,Object> closure){
        iterateXSD(xsd.getDocumentElement(),closure);
    }

    /**
     *
     * @param xsd
     * @param closure
     */
    public static void iterateXSD(Element xsd, Closure<XSDIterateBean,Object> closure){

        //DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();

        XSImplementation impl =
                //(XSImplementation) registry.getDOMImplementation("XS-Loader");
                new XSImplementationImpl();
        XSLoader schemaLoader = impl.createXSLoader(null);

        XSModel model = schemaLoader.load(new DOMInputImpl(null, null, null, XMLFormat.toString(xsd), null));

        //root elements
        XSNamedMap elements = model.getComponents(XSConstants.ELEMENT_DECLARATION);
        for(int i=0;i<elements.getLength();i++){
            XSElementDecl e = ((XSElementDecl)elements.item(i));
            processEl(null,null,e,closure);
        }

        //types
        XSNamedMap types = model.getComponents(XSConstants.TYPE_DEFINITION);
        for(int i=0;i<types.getLength();i++){
            XSTypeDefinition e = ((XSTypeDefinition)types.item(i));
            if(!"http://www.w3.org/2001/XMLSchema".equals(e.getNamespace())){
                closure.callQuite(new XSDIterateBean(null,e));
                if(e instanceof XSComplexTypeDecl){
                    processCT("#"+e.getName().replace(".","\\."),(XSComplexTypeDecl)e,closure);
                }
            }

        }
    }

    private static void processModelGroup(String path, XSModelGroup mg, Closure<XSDIterateBean,Object> closure){
        for(int i=0;i<mg.getParticles().getLength();i++){
            if(mg.getParticles().item(i) instanceof XSParticleDecl){
                XSParticleDecl p = (XSParticleDecl)mg.getParticles().item(i);
                if(p.getTerm() instanceof XSElementDecl){
                    processEl(path,p,(XSElementDecl)p.getTerm(),closure);
                }//else if(p.getTerm() instanceof XSGroupDecl){
                    //println("---------")
                    //def gr = (XSGroupDecl)p.getTerm();
                    //processModelGroup(tab,gr.getModelGroup());
                //}
            }
        }
    }

    private static void processCT(String path, XSComplexTypeDecl ct, Closure<XSDIterateBean,Object> closure){
        //process base complex types
        if(ct.getBaseType()!=null && ct.getBaseType() instanceof XSComplexTypeDecl
                && ct != ct.getBaseType()){
            //println(XSConstants.DERIVATION_EXTENSION+":"+ct.getDerivationMethod())
            processCT(path, (XSComplexTypeDecl)ct.getBaseType(),closure);
        }

        //process this type
        if(ct.getParticle()!=null){
            closure.callQuite(new XSDIterateBean(path,(XSParticleDecl)ct.getParticle()));

            //println(ct.getParticle().getTerm().getClass())
            //model group
            if(ct.getParticle().getTerm() instanceof XSModelGroup){
                XSModelGroup mg = (XSModelGroup)ct.getParticle().getTerm();
                //println(mg.getCompositor()+"::"+XSModelGroup.COMPOSITOR_ALL);
                //println(ct.getParticle().getMaxOccurs())
                processModelGroup(path,mg,closure);
            }
        }

        //process attributes
        for(int i=0;i<ct.getAttrGrp().getAttributeUses().getLength();i++){
            XSAttributeUse attr = (XSAttributeUse) ct.getAttrGrp().getAttributeUses().item(i);
            //def a = ((XSAttributeUse)ct.getAttrGrp().getAttributeUses().item(i)).getAttrDeclaration();
            //def req = ((XSAttributeUse)ct.getAttrGrp().getAttributeUses().item(i)).getRequired();
            String p = path+".@"+attr.getAttrDeclaration().getName().replace(".","\\.");
            closure.callQuite(new XSDIterateBean(p,attr));
        }
    }

    private static void processEl(String path, XSParticleDecl part, XSElementDecl el, Closure<XSDIterateBean,Object> closure){
        boolean root = Objects.isNullOrEmpty(path);
        if(!root)
            path = path+"."+el.getName().replace(".","\\.");
        else
            path = el.getName().replace(".","\\.");

        //call closure
        if(!root) //not root element
            closure.callQuite(new XSDIterateBean(path,part));
        else //root element
            closure.callQuite(new XSDIterateBean(path,el));

        //println(el.getTypeDefinition())
        if(el.getTypeDefinition() instanceof XSComplexTypeDecl){
            //complex type
            //if(part)
            //    println(tab+el.getName()+":"+part.getMinOccurs()+":"+part.getMaxOccurs());
            //else
            //    println(tab+el.getName());
            //ct
            XSComplexTypeDecl ct = (XSComplexTypeDecl)el.getTypeDefinition();
            if(ct.getAnonymous())
                processCT(path,ct,closure);
        }else{
            //simple type
            //XSSimpleTypeDecl st = (XSSimpleTypeDecl)el.getTypeDefinition();
            //println(tab+el.getName()+":"+type.getName());
        }
    }

    /**
     *
     */
    public static class XSDIterateBean{
        private String path;
        private XSObject object;

        public XSDIterateBean(String path, XSObject object) {
            this.path = path;
            this.object = object;
        }

        public XSObject getObject() {
            return object;
        }

        public String getPath() {
            return path;
        }

        public XSTypeDefinition getTypeDefinition() {
            return (XSTypeDefinition)object;
        }

        public XSParticleDecl getParticle() {
            return (XSParticleDecl)object;
        }

        public XSElementDecl getElementDeclaration() {
            return (XSElementDecl)object;
        }

        public XSAttributeUse getAttributeUse() {
            return (XSAttributeUse)object;
        }
    }

}
