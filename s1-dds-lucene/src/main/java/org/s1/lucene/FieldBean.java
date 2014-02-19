package org.s1.lucene;

import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexableField;
import org.s1.objects.Objects;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;

/**
 * Full text index field definition
 */
public class FieldBean {

    private Class type;
    private float boost;
    private boolean text;
    private boolean stored;
    private boolean indexed;

    /**
     *
     * @param type
     * @param boost
     */
    public FieldBean(Class type, float boost) {
        this(type,boost,false,true,true);
    }

    /**
     *
     * @param type
     * @param boost
     * @param text
     */
    public FieldBean(Class type, float boost, boolean text) {
        this(type,boost,text,true,true);
    }

    /**
     *
     * @param type
     * @param boost
     * @param text
     * @param stored
     * @param indexed
     */
    public FieldBean(Class type, float boost, boolean text, boolean stored, boolean indexed) {
        this.type = type;
        this.boost = boost;
        this.text = text;
        this.stored = stored;
        this.indexed = indexed;
    }

    /**
     *
     * @return
     */
    public Class getType() {
        return type;
    }

    /**
     *
     * @param type
     */
    public void setType(Class type) {
        this.type = type;
    }

    /**
     *
     * @return
     */
    public float getBoost() {
        return boost;
    }

    /**
     *
     * @param boost
     */
    public void setBoost(float boost) {
        this.boost = boost;
    }

    /**
     *
     * @return
     */
    public boolean isText() {
        return text;
    }

    /**
     *
     * @param text
     */
    public void setText(boolean text) {
        this.text = text;
    }

    /**
     *
     * @return
     */
    public boolean isStored() {
        return stored;
    }

    /**
     *
     * @param stored
     */
    public void setStored(boolean stored) {
        this.stored = stored;
    }

    /**
     *
     * @return
     */
    public boolean isIndexed() {
        return indexed;
    }

    /**
     *
     * @param indexed
     */
    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }

    /**
     *
     * @param m
     */
    public void fromMap(Map<String,Object> m){
        String cls = Objects.get(m,"type");
        this.type = Objects.resolveType(cls);
        this.boost = Objects.get(Float.class, m,"boost",1.0F);
        this.text = Objects.get(Boolean.class, m,"text",false);
        this.stored = Objects.get(Boolean.class, m,"stored",true);
        this.indexed = Objects.get(Boolean.class, m,"indexed",true);
    }

    /**
     * 
     * @param name
     * @param value
     * @return
     */
    public Field toField(String name, Object value){
        Field field = null;
        value = Objects.cast(value,type);

        FieldType ft = new FieldType();
        ft.setIndexed(indexed);
        ft.setStored(stored);
        if(type == Long.class || type == BigInteger.class){
            ft.setNumericType(FieldType.NumericType.LONG);
            field = new LongField(name, Objects.cast(value, Long.class), ft);
        }else if(type == Double.class || type == BigDecimal.class){
            ft.setNumericType(FieldType.NumericType.DOUBLE);
            field = new DoubleField(name,Objects.cast(value,Double.class), ft);
        }else if(type == Integer.class){
            ft.setNumericType(FieldType.NumericType.INT);
            field = new IntField(name,(int)Objects.cast(value,Integer.class), ft);
        }else if(type == Float.class){
            ft.setNumericType(FieldType.NumericType.FLOAT);
            field = new FloatField(name,Objects.cast(value,Float.class), ft);
        }else if(type == Boolean.class){
            ft.setNumericType(FieldType.NumericType.INT);
            field = new IntField(name,(Boolean)value?1:0, ft);
        }else if(type == Date.class){
            ft.setNumericType(FieldType.NumericType.LONG);
            field = new LongField(name,((Date)value).getTime(), ft);
        }else if(type == String.class){
            if(text){
                if(stored){
                    field = new TextField(name, (String)value, Field.Store.YES);
                }else{
                    field = new TextField(name, (String)value, Field.Store.NO);
                }
            }else{
                if(stored){
                    field = new StringField(name, (String)value, Field.Store.YES);
                }else{
                    field = new StringField(name, (String)value, Field.Store.NO);
                }
            }
        }
        return field;
    }

    /**
     *
     * @param field
     * @return
     */
    public Object toValue(IndexableField field){
        Object val = null;
        if(type == Integer.class){
            val = Objects.cast(field.numericValue(),type);
        }else if(type == Long.class){
            val = Objects.cast(field.numericValue(),type);
        }else if(type == Float.class){
            val = Objects.cast(field.numericValue(),type);
        }else if(type == Double.class){
            val = Objects.cast(field.numericValue(),type);
        }else if(type == BigInteger.class){
            val = Objects.cast(field.numericValue(),type);
        }else if(type == BigDecimal.class){
            val = Objects.cast(field.numericValue(),type);
        }else if(type == Boolean.class){
            val = Objects.equals(field.numericValue(),1);
        }else if(type == Date.class){
            val = new Date(Objects.cast(field.numericValue(),Long.class));
        }else if(type == String.class){
            val = field.stringValue();
        }
        return val;
    }
}
