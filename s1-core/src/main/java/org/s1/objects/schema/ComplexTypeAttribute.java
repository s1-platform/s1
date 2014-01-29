package org.s1.objects.schema;

import org.s1.objects.Objects;

import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 11.01.14
 * Time: 17:17
 */
public class ComplexTypeAttribute extends ObjectSchemaAttribute<Map<String,Object>> {

    protected Class<? extends ComplexType> typeClass;
    protected Map<String,Object> typeCfg;

    ComplexTypeAttribute(){
    }

    public ComplexTypeAttribute(String name, String label, Class<? extends ComplexType> typeClass) {
        super(name,label,"ComplexType");
        this.typeClass = typeClass;
    }

    public ComplexTypeAttribute(String name, String label, Class<? extends ComplexType> typeClass, Map<String, Object> typeCfg) {
        this(name,label,typeClass);
        this.typeCfg = Objects.copy(typeCfg);
    }

    @Override
    protected void validateType(boolean expand, boolean deep,  Map<String,Object> ctx, boolean quite) throws Exception{
        if(data!=null && !(data instanceof Map)){
            throw new Exception("Data is not map");
        }

        if(data!=null){
            ComplexType t = typeClass.newInstance();
            t.setCfg(typeCfg);
            data = t.validate(data);
            if(expand)
                data = t.expand(data,deep);
        }
    }

    @Override
    void fromMap(Map<String, Object> m) throws ObjectSchemaFormatException {
        super.fromMap(m);
        this.type = "ComplexType";
        String tc = (String)Objects.get(m,"typeClass");
        try {
            this.typeClass = (Class<? extends ComplexType>)Class.forName(tc);
        } catch (Exception e) {
            throw new ObjectSchemaFormatException("Cannot set property typeClass ("+tc+") to ComplexType ("+getPath(" / ")+")",e);
        }
        this.typeCfg = Objects.newHashMap();
        this.typeCfg = Objects.get(m, "typeCfg", this.typeCfg);
        this.typeCfg = Objects.copy(this.typeCfg);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> m = super.toMap();
        m.put("typeClass",this.typeClass.getName());
        m.put("typeCfg",Objects.copy(this.typeCfg));
        return m;
    }

    public Class<? extends ComplexType> getTypeClass() {
        return typeClass;
    }

    public Map<String, Object> getTypeCfg() {
        return typeCfg;
    }

    public void setTypeClass(Class<? extends ComplexType> typeClass) {
        this.typeClass = typeClass;
    }

    public void setTypeCfg(Map<String, Object> typeCfg) {
        if(typeCfg!=null)
            this.typeCfg = typeCfg;
        else
            this.typeCfg = Objects.newHashMap();
    }
}
