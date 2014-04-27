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
public class XSD2ArraysList {

    public static List<String> toArraysList(Document xsd){
        return toArraysList(xsd.getDocumentElement());
    }

    public static List<String> toArraysList(Element xsd){
        final List<String> listsPath = Objects.newArrayList();
        if(xsd!=null){
            XSDIterator.iterateXSD(xsd, new Closure<XSDIterator.XSDIterateBean, Object>() {
                @Override
                public Object call(XSDIterator.XSDIterateBean input)  {
                    if (input.getObject() instanceof XSParticleDecl) {
                        XSTerm term = input.getParticle().getTerm();
                        if (term instanceof XSElementDecl) {
                            if (input.getParticle().getMaxOccursUnbounded() || input.getParticle().getMaxOccurs() > 1)
                                listsPath.add(input.getPath());
                        } else if (term instanceof XSModelGroup) {
                            //println("Compositor type: "+term.getCompositor())
                        }
                    } else if (input.getObject() instanceof XSTypeDefinition) {

                    } else if (input.getObject() instanceof XSElementDecl) {
                        //println("Root element: "+part.getName())
                    } if (input.getObject() instanceof XSAttributeUse) {
                        //println("attr:"+input.getPath()+":"+part.getAttrDeclaration().getName()+":"+getBaseSimpleType(part.getAttrDeclaration().getTypeDefinition()))
                    }
                    return null;
                }
            });
        }
        return listsPath;
    }
    
}
