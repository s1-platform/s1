package org.s1.objects.schema;

import org.s1.objects.Objects;

import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 13.01.14
 * Time: 21:20
 */
public abstract class ComplexType {

    protected Map<String,Object> cfg = Objects.newHashMap();

    public Map<String, Object> getCfg() {
        return cfg;
    }

    public void setCfg(Map<String, Object> cfg) {
        if(cfg==null)
            cfg = Objects.newHashMap();
        this.cfg = cfg;
    }

    /**
     *
     * @param m
     * @return
     * @throws Exception
     */
    public abstract Map<String, Object> expand(Map<String, Object> m, boolean expand) throws Exception;

    /**
     *
     * @param m
     * @return
     * @throws Exception
     */
    public abstract Map<String, Object> validate(Map<String, Object> m) throws Exception;

}
