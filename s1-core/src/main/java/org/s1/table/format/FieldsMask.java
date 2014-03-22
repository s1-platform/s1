package org.s1.table.format;

import org.s1.objects.Objects;

import java.util.List;
import java.util.Map;

/**
 * Fields mask
 */
public class FieldsMask {

    private boolean show;

    private List<String> fields;

    /**
     *
     */
    public FieldsMask() {
    }

    /**
     *
     * @param show
     * @param fields
     */
    public FieldsMask(boolean show, List<String> fields) {
        this.show = show;
        this.fields = fields;
    }

    /**
     *
     * @param show
     * @param fields
     */
    public FieldsMask(boolean show, String ... fields) {
        this(show, Objects.newArrayList(fields));
    }

    /**
     *
     * @return
     */
    public Map<String,Object> toMap(){
        return Objects.newHashMap(String.class,Object.class,
                "show",show,
                "fields",fields);
    }

    /**
     *
     */
    public void fromMap(Map<String,Object> m){
        show = Objects.get(Boolean.class,m,"show",false);
        fields = Objects.get(m,"fields");
    }

    /**
     *
     * @return
     */
    public boolean isShow() {
        return show;
    }

    /**
     *
     * @param show
     */
    public void setShow(boolean show) {
        this.show = show;
    }

    /**
     *
     * @return
     */
    public List<String> getFields() {
        return fields;
    }

    /**
     *
     * @param fields
     */
    public void setFields(List<String> fields) {
        this.fields = fields;
    }
}
