package org.s1.table.format;

import org.s1.objects.Objects;

import java.util.Map;

/**
 * Sort format
 */
public class Sort {

    private String name;

    private boolean desc;

    /**
     *
     */
    public Sort() {
    }

    /**
     *
     * @param name
     * @param desc
     */
    public Sort(String name, boolean desc) {
        this.name = name;
        this.desc = desc;
    }

    /**
     *
     * @return
     */
    public Map<String,Object> toMap(){
        return Objects.newHashMap(String.class, Object.class,
                "desc", desc,
                "name", name);
    }

    /**
     *
     */
    public void fromMap(Map<String,Object> m){
        desc = Objects.get(Boolean.class,m,"desc",false);
        name = Objects.get(m,"name");
    }

    /**
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return
     */
    public boolean isDesc() {
        return desc;
    }

    /**
     *
     * @param desc
     */
    public void setDesc(boolean desc) {
        this.desc = desc;
    }
}
