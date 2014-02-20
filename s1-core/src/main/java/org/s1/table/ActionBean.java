package org.s1.table;

import org.s1.misc.Closure;
import org.s1.objects.schema.ObjectSchema;

import java.util.Map;

/**
 * Table action
 */
public class ActionBean {

    private String name;
    private String label;
    private String from;
    private String to;
    private Closure<Map<String,Object>,Boolean> access;
    private Closure<BeforeBean,Object> before;
    private Closure<AfterBean,Object> after;
    private ObjectSchema schema;
    private ObjectSchema foundationSchema;
    private boolean log;
    private Closure<MergeBean,Object> merge;

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
    public String getLabel() {
        return label;
    }

    /**
     *
     * @param label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     *
     * @return
     */
    public String getFrom() {
        return from;
    }

    /**
     *
     * @param from
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     *
     * @return
     */
    public String getTo() {
        return to;
    }

    /**
     *
     * @param to
     */
    public void setTo(String to) {
        this.to = to;
    }

    /**
     *
     * @return
     */
    public Closure<Map<String, Object>, Boolean> getAccess() {
        return access;
    }

    /**
     *
     * @param access
     */
    public void setAccess(Closure<Map<String, Object>, Boolean> access) {
        this.access = access;
    }

    /**
     *
     * @return
     */
    public Closure<BeforeBean, Object> getBefore() {
        return before;
    }

    /**
     *
     * @param before
     */
    public void setBefore(Closure<BeforeBean, Object> before) {
        this.before = before;
    }

    /**
     *
     * @return
     */
    public Closure<AfterBean, Object> getAfter() {
        return after;
    }

    /**
     *
     * @param after
     */
    public void setAfter(Closure<AfterBean, Object> after) {
        this.after = after;
    }

    /**
     *
     * @return
     */
    public Closure<MergeBean, Object> getMerge() {
        return merge;
    }

    /**
     *
     * @param merge
     */
    public void setMerge(Closure<MergeBean, Object> merge) {
        this.merge = merge;
    }

    /**
     *
     * @return
     */
    public ObjectSchema getSchema() {
        return schema;
    }

    /**
     *
     * @param schema
     */
    public void setSchema(ObjectSchema schema) {
        this.schema = schema;
    }

    /**
     *
     * @return
     */
    public ObjectSchema getFoundationSchema() {
        return foundationSchema;
    }

    /**
     *
     * @param foundationSchema
     */
    public void setFoundationSchema(ObjectSchema foundationSchema) {
        this.foundationSchema = foundationSchema;
    }

    /**
     *
     * @return
     */
    public boolean isLog() {
        return log;
    }

    /**
     *
     * @param log
     */
    public void setLog(boolean log) {
        this.log = log;
    }

    /**
     *
     */
    public static class BeforeBean{
        private Map<String,Object> data;
        private Map<String,Object> foundation;
        private ActionBean action;
        private Map<String,Object> oldObject;

        /**
         *
         * @param data
         * @param foundation
         * @param action
         * @param oldObject
         */
        public BeforeBean(Map<String, Object> data, Map<String, Object> foundation, ActionBean action, Map<String, Object> oldObject) {
            this.data = data;
            this.foundation = foundation;
            this.action = action;
            this.oldObject = oldObject;
        }

        /**
         *
         * @return
         */
        public Map<String, Object> getData() {
            return data;
        }

        /**
         *
         * @return
         */
        public Map<String, Object> getOldObject() {
            return oldObject;
        }

        /**
         *
         * @return
         */
        public Map<String, Object> getFoundation() {
            return foundation;
        }

        /**
         *
         * @return
         */
        public ActionBean getAction() {
            return action;
        }
    }

    /**
     *
     */
    public static class AfterBean extends BeforeBean{
        private Map<String,Object> newObject;

        /**
         *
         * @param data
         * @param foundation
         * @param action
         * @param oldObject
         * @param newObject
         */
        public AfterBean(Map<String, Object> data, Map<String, Object> foundation, ActionBean action, Map<String, Object> oldObject, Map<String, Object> newObject) {
            super(data, foundation, action, oldObject);
            this.newObject = newObject;
        }
    }

    /**
     *
     */
    public static class MergeBean {
        private Map<String,Object> result;
        private Map<String,Object> oldObject;
        private Map<String,Object> data;

        /**
         *
         * @param result
         * @param oldObject
         * @param data
         */
        public MergeBean(Map<String, Object> result, Map<String, Object> oldObject, Map<String, Object> data) {
            this.result = result;
            this.oldObject = oldObject;
            this.data = data;
        }

        /**
         *
         * @return
         */
        public Map<String, Object> getResult() {
            return result;
        }

        /**
         *
         * @return
         */
        public Map<String, Object> getOldObject() {
            return oldObject;
        }

        /**
         *
         * @return
         */
        public Map<String, Object> getData() {
            return data;
        }
    }
}
