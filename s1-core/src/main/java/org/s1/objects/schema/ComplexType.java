package org.s1.objects.schema;

import org.s1.objects.Objects;

import java.util.Map;

/**
 * Base type for complex types
 */
public abstract class ComplexType {

    protected Map<String,Object> config = Objects.newHashMap();

    /**
     *
     * @return
     */
    public Map<String, Object> getConfig() {
        return config;
    }

    /**
     *
     * @param config
     */
    public void setConfig(Map<String, Object> config) {
        if(config ==null)
            config = Objects.newHashMap();
        this.config = config;
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
