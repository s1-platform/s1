package org.s1.objects.schema;

import org.s1.S1SystemError;
import org.s1.objects.Objects;
import org.s1.misc.Closure;
import org.s1.script.Context;
import org.s1.script.S1ScriptEngine;
import org.s1.script.ScriptException;
import org.s1.script.ScriptFunction;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 11.01.14
 * Time: 17:17
 */
public abstract class ObjectSchemaAttribute<T> {

    /**
     * Create attribute from {@link java.util.Map}
     *
     * @param a
     * @return
     */
    public static ObjectSchemaAttribute createFromMap(Map<String,Object> a) throws ObjectSchemaFormatException{
        ObjectSchemaAttribute attr = null;
        String type = Objects.get(a,"type","Object");
        if("Map".equals(type)){
            attr = new MapAttribute();
        }else if("List".equals(type)){
            attr = new ListAttribute();
        }else if("ComplexType".equals(type)){
            attr = new ComplexTypeAttribute();
        }else if(type.startsWith("#")){
            attr = new ReferenceAttribute();
        }else{
            attr = new SimpleTypeAttribute();
        }
        attr.fromMap(a);
        return attr;
    }

    ObjectSchemaAttribute(){

    }

    ObjectSchemaAttribute(String name, String label, String type){
        this.name = name;
        this.label = label;
        this.type = type;
    }

    ObjectSchemaAttribute(Map<String, Object> a) throws ObjectSchemaFormatException{
        fromMap(a);
    }

    private Map<String,Object> validationCtx;

    protected boolean required;
    protected boolean denied;
    protected boolean nonPresent;

    protected String type;
    protected String name;
    protected String label;
    protected String description;

    protected Closure<ObjectSchemaAttribute,ObjectSchemaAttribute> script;
    protected Closure<ObjectSchemaAttribute,String> validate;

    protected T def;

    protected Map<String,Object> hint;

    protected List<Object> variants;

    protected String error;
    protected T data;

    /**
     * parent attribute
     */
    protected ObjectSchemaAttribute parent;

    /**
     * schema
     */
    protected ObjectSchema schema;

    /**
     *
     * @param parent
     */
    void setParent(ObjectSchemaAttribute parent) {
        this.parent = parent;
    }

    /**
     *
     * @param schema
     */
    void setSchema(ObjectSchema schema) {
        this.schema = schema;
        updateChildSchemaAndParent();
    }

    /**
     * Get parent attribute
     * @return
     */
    public ObjectSchemaAttribute getParent() {
        return parent;
    }

    /**
     * Get schema reference
     * @return
     */
    public ObjectSchema getSchema() {
        return schema;
    }

    /**
     *
     * @param delimiter
     * @return
     */
    public String getPath(String delimiter){
        ObjectSchemaAttribute a = this;
        String path = "";
        while(a!=null && a.getParent()!=null){
            path = a.getLabel()+path;
            a = a.getParent();
            if(a!=null && a.getParent()!=null)
                path = delimiter+path;
        }
        return path;
    }

    /**
     * Update child schema and parent references
     */
    protected void updateChildSchemaAndParent(){

    }

    /**
     * Set attribute properties from {@link java.util.Map}
     *
     * @param m
     */
    void fromMap(Map<String,Object> m) throws ObjectSchemaFormatException{
        this.error = null;
        this.data = null;
        this.name = Objects.get(m, "name");
        this.label = Objects.get(m, "label");
        this.description = Objects.get(m, "description");
        this.type = Objects.get(m, "type", "Object");

        this.required = "required".equals(Objects.get(m, "appearance"));
        this.denied = "denied".equals(Objects.get(m, "appearance"));
        this.nonPresent = "nonPresent".equals(Objects.get(m, "appearance"));

        this.def = Objects.get(m, "default");
        this.hint = Objects.get(m, "hint");

        List<Object> vars = Objects.get(m, "variants");
        this.variants = Objects.newArrayList();
        if(!Objects.isNullOrEmpty(vars)){
            vars = Objects.copy(vars);
            for(Object a: vars){
                variants.add(Objects.cast(a,this.type));
            }
        }

        final S1ScriptEngine scriptEngine = new S1ScriptEngine("objectSchema.scriptEngine");
        final String script = Objects.get(m, "script");
        if(!Objects.isNullOrEmpty(script)){
            this.script = new Closure<ObjectSchemaAttribute, ObjectSchemaAttribute>() {
                @Override
                public ObjectSchemaAttribute call(final ObjectSchemaAttribute input) {
                    //call script
                    Map<String,Object> m = input.getScriptMap();
                    scriptEngine.eval(script,m);
                    Map<String,Object> attr = Objects.get(m,"attr");
                    try {
                        return ObjectSchemaAttribute.createFromMap(attr);
                    } catch (ObjectSchemaFormatException e) {
                        throw S1SystemError.wrap(e);
                    }
                }
            };
        }

        final String validate = Objects.get(m, "validate");
        if(!Objects.isNullOrEmpty(validate)){
            this.validate = new Closure<ObjectSchemaAttribute, String>() {
                @Override
                public String call(ObjectSchemaAttribute input) {
                    Map<String,Object> m = input.getScriptMap();
                    try{
                        scriptEngine.eval(validate,m);
                    }catch (ScriptException e){
                        return ""+e.getData();
                    }
                    return null;
                }
            };
        }
    }

    /**
     *
     * @return
     */
    protected Map<String,Object> getParentMap(){
        Map<String,Object> m = null;
        if(getParent()!=null){
            m = Objects.newHashMap();
            m.putAll(getParent().toMap());
            m.put("parent",getParent().getParentMap());
            m.put("data",getParent().getData());
        }
        return m;
    }

    /**
     *
     * @return
     */
    protected Map<String,Object> getScriptMap(){
        Map<String,Object> m = Objects.newHashMap();
        m.put("data", getData());
        m.put("parent", getParentMap());
        m.put("attr", toMap());
        m.put("ctx", null);
        return m;
    }

    /**
     * Format to {@link java.util.Map}
     *
     * @return
     */
    public Map<String,Object> toMap(){
        Map<String,Object> m = Objects.newHashMap("name", name,
                "label", label,
                "description", description,
                "type", type,
                "appearance", required ? "required" : (denied ? "denied" : (nonPresent ? "nonPresent" : "normal")),
                "default", def,
                "hint", hint,
                "variants", Objects.copy(variants),
                "data", Objects.copy(data));
        if(error!=null){
            m.put("error",error);
        }
        return m;
    }

    public ObjectSchemaAttribute copyAndReset(){
        ObjectSchemaAttribute a = null;
        try {
            a = ObjectSchemaAttribute.createFromMap(toMap()).setValidate(validate).setScript(script);;
        } catch (ObjectSchemaFormatException e) {
            e.printStackTrace();
        }
        return a;
    }

    public T getData() {
        return data;
    }

    void setData(T data) {
        this.data = data;
    }

    public ObjectSchemaAttribute setName(String p){
        this.name = p;
        return this;
    }

    public ObjectSchemaAttribute setLabel(String p){
        this.label = p;
        return this;
    }

    public ObjectSchemaAttribute setDescription(String p){
        this.description = p;
        return this;
    }

    public ObjectSchemaAttribute setRequired(boolean p){
        this.required = p;
        return this;
    }

    public ObjectSchemaAttribute setDenied(boolean p){
        this.denied = p;
        return this;
    }

    public ObjectSchemaAttribute setNonPresent(boolean p){
        this.nonPresent = p;
        return this;
    }

    public ObjectSchemaAttribute setScript(Closure<ObjectSchemaAttribute,ObjectSchemaAttribute> p){
        this.script = p;
        return this;
    }

    public ObjectSchemaAttribute setValidate(Closure<ObjectSchemaAttribute,String> p){
        this.validate = p;
        return this;
    }

    public ObjectSchemaAttribute setDefault(T p){
        this.def = p;
        return this;
    }

    public ObjectSchemaAttribute setHint(Map<String, Object> p){
        this.hint = p;
        return this;
    }

    public ObjectSchemaAttribute setVariants(List<Object> p){
        this.variants = Objects.copy(p);
        return this;
    }

    public ObjectSchemaAttribute setVariants(Object ... args){
        this.variants = Arrays.asList(args);
        return this;
    }

    public String getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isDenied() {
        return denied;
    }

    public boolean isNonPresent() {
        return nonPresent;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public Closure<ObjectSchemaAttribute,ObjectSchemaAttribute> getScript() {
        return script;
    }

    public Closure<ObjectSchemaAttribute,String> getValidate() {
        return validate;
    }

    public T getDefault() {
        return def;
    }

    public List<Object> getVariants() {
        return variants;
    }

    public Map<String, Object> getHint() {
        return hint;
    }

    public String getError() {
        return error;
    }

    /**
     * Validate attribute
     *
     * @param data
     * @param expand
     * @param deep
     * @param ctx
     * @param quite
     * @return
     */
    ObjectSchemaAttribute validate(T data, boolean expand, boolean deep,
                  Map<String,Object> ctx, boolean quite)  throws ObjectSchemaValidationException{
        ObjectSchemaAttribute attribute = this;
        attribute.error = null;
        attribute.validationCtx = ctx;
        try{
            String name = attribute.name;
            attribute.data = data;
            //run script
            if(attribute.script!=null){
                ObjectSchemaAttribute a1 = (ObjectSchemaAttribute)attribute.script.call(attribute);
                if(a1!=null){
                    a1.setParent(attribute.parent);
                    a1.setSchema(attribute.schema);
                    attribute = a1;
                }
                attribute.name = name;
                attribute.data = data;
                attribute.validationCtx = ctx;
            }

            if(!attribute.nonPresent){

                //default
                if((attribute.data==null) && attribute.def!=null){
                    attribute.data = attribute.def;
                }

                if(!quite){
                    //check required, denied
                    if(attribute.required && (attribute.data==null)){
                        throw new Exception("Attribute is required");
                    }
                    if(attribute.denied && (attribute.data!=null)){
                        throw new Exception("Attribute is denied");
                    }
                    if(attribute.denied){
                        attribute.data = null;
                        return attribute;
                    }
                }

                if(attribute instanceof ReferenceAttribute){
                    attribute = ((ReferenceAttribute)attribute).resolve();
                }

                //validate type
                attribute.validateType(expand, deep, ctx, quite);

                //variants
                if(attribute.data!=null && !Objects.isNullOrEmpty(attribute.variants)){
                    final Object data1 = attribute.data;
                    final String type1 = attribute.type;
                    if(Objects.find(attribute.variants, new Closure<Object, Boolean>() {
                        @Override
                        public Boolean call(Object input) {
                            return Objects.equals(input, Objects.cast(data1, type1));
                        }
                    })==null){
                        throw new Exception("Value not in available variants");
                    }
                }

                if(quite){
                    //check required, denied
                    if(attribute.required && (attribute.data==null)){
                        throw new Exception("Attribute is required");
                    }
                    if(attribute.denied && (attribute.data!=null)){
                        throw new Exception("Attribute is denied");
                    }
                    if(attribute.denied){
                        attribute.data = null;
                        return attribute;
                    }
                }

                //validate script
                if(attribute.validate!=null){
                    String msg = (String)attribute.validate.call(attribute);
                    if(!Objects.isNullOrEmpty(msg))
                        throw new Exception(msg);
                }
            }
        }catch (Throwable e){
            if(quite)
                attribute.error = e.getMessage();
            else{
                if(e instanceof ObjectSchemaValidationException)
                    throw new ObjectSchemaValidationException(e.getMessage(),e);
                else
                    throw new ObjectSchemaValidationException(getPath(" / ")+": "+e.getMessage(),e);
            }
        }
        return attribute;
    }

    /**
     * Validate attribute data
     *
     * @param expand
     * @param deep
     * @param ctx
     * @param quite
     * @throws Exception
     */
    protected abstract void validateType(boolean expand, boolean deep,  Map<String,Object> ctx, boolean quite) throws Exception;

}
