package org.s1.table;

import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;
import org.s1.objects.schema.ObjectSchema;
import org.s1.objects.schema.ObjectSchemaFormatException;
import org.s1.script.S1ScriptEngine;
import org.s1.script.ScriptException;

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
    public Map<String,Object> toMap(){
        return Objects.newHashMap(
            "name",name,
                "label",label,
                "from",from,
                "to",to,
                "schema",schema==null?null:schema.toMap(),
                "foundationSchema",foundationSchema==null?null:foundationSchema.toMap(),
                "log",log
        );
    }

    /**
     *
     * @param it
     */
    public void fromMap(final S1ScriptEngine scriptEngine, Map<String,Object> it) throws ObjectSchemaFormatException{
        this.setName(Objects.get(String.class, it, "name"));
        this.setLabel(Objects.get(String.class, it, "label"));

        this.setFrom(Objects.get(String.class, it, "from"));
        this.setTo(Objects.get(String.class,it,"to"));

        final String accessStr = Objects.get(it,"access","").trim();
        if(!Objects.isNullOrEmpty(accessStr)){
            this.setAccess(new Closure<Map<String, Object>, Boolean>() {
                @Override
                public Boolean call(Map<String, Object> input) throws ClosureException {
                    try{
                        return scriptEngine.evalInFunction(Boolean.class, accessStr, Objects.newHashMap(String.class, Object.class,
                                "record", input));
                    }catch (ScriptException e){
                        throw ClosureException.wrap(e);
                    }
                }
            });
        }

        final String beforeStr = Objects.get(it,"before","").trim();
        if(!Objects.isNullOrEmpty(beforeStr)){
            this.setBefore(new Closure<ActionBean.BeforeBean, Object>() {
                @Override
                public Object call(ActionBean.BeforeBean input) throws ClosureException {
                    try {
                        scriptEngine.eval(beforeStr, Objects.newHashMap(String.class, Object.class,
                                "oldRecord", input.getOldRecord(),
                                "foundation", input.getFoundation(),
                                "data", input.getData()));
                    } catch (ScriptException e) {
                        throw ClosureException.wrap(e);
                    }
                    return null;
                }
            });
        }

        final String afterStr = Objects.get(it,"after","").trim();
        if(!Objects.isNullOrEmpty(afterStr)){
            this.setAfter(new Closure<ActionBean.AfterBean, Object>() {
                @Override
                public Object call(ActionBean.AfterBean input) throws ClosureException {
                    try {
                        scriptEngine.eval(afterStr, Objects.newHashMap(String.class, Object.class,
                                "newRecord", input.getNewRecord(),
                                "oldRecord", input.getOldRecord(),
                                "foundation", input.getFoundation(),
                                "data", input.getData()));
                    } catch (ScriptException e) {
                        throw ClosureException.wrap(e);
                    }
                    return null;
                }
            });
        }

        final String mergeStr = Objects.get(it,"merge","").trim();
        if(!Objects.isNullOrEmpty(mergeStr)){
            this.setMerge(new Closure<ActionBean.MergeBean, Object>() {
                @Override
                public Object call(ActionBean.MergeBean input) throws ClosureException {
                    try {
                        scriptEngine.eval(mergeStr, Objects.newHashMap(String.class, Object.class,
                                "newRecord", input.getNewRecord(),
                                "oldRecord", input.getOldRecord(),
                                "data", input.getData()));
                    } catch (ScriptException e) {
                        throw ClosureException.wrap(e);
                    }
                    return null;
                }
            });
        }

        Map<String,Object> sm = Objects.get(it,"schema");
        if(!Objects.isNullOrEmpty(sm)){
            ObjectSchema s = new ObjectSchema();
            s.fromMap(sm);
            this.setSchema(s);
        }

        //log
        this.setLog(Objects.get(Boolean.class, it, "log"));
        Map<String,Object> fsm = Objects.get(it,"foundationSchema");
        if(!Objects.isNullOrEmpty(fsm)){
            ObjectSchema fs = new ObjectSchema();
            fs.fromMap(fsm);
            this.setFoundationSchema(fs);
        }
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
        private Map<String,Object> oldRecord;

        /**
         *
         * @param data
         * @param foundation
         * @param action
         * @param oldRecord
         */
        public BeforeBean(Map<String, Object> data, Map<String, Object> foundation, ActionBean action, Map<String, Object> oldRecord) {
            this.data = data;
            this.foundation = foundation;
            this.action = action;
            this.oldRecord = oldRecord;
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
        public Map<String, Object> getOldRecord() {
            return oldRecord;
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
        private Map<String,Object> newRecord;

        /**
         *
         * @param data
         * @param foundation
         * @param action
         * @param oldObject
         * @param newRecord
         */
        public AfterBean(Map<String, Object> data, Map<String, Object> foundation, ActionBean action, Map<String, Object> oldObject, Map<String, Object> newRecord) {
            super(data, foundation, action, oldObject);
            this.newRecord = newRecord;
        }

        /**
         *
         * @return
         */
        public Map<String, Object> getNewRecord() {
            return newRecord;
        }
    }

    /**
     *
     */
    public static class MergeBean {
        private Map<String,Object> newRecord;
        private Map<String,Object> oldRecord;
        private Map<String,Object> data;

        /**
         *
         * @param newRecord
         * @param oldRecord
         * @param data
         */
        public MergeBean(Map<String, Object> newRecord, Map<String, Object> oldRecord, Map<String, Object> data) {
            this.newRecord = newRecord;
            this.oldRecord = oldRecord;
            this.data = data;
        }

        /**
         *
         * @return
         */
        public Map<String, Object> getNewRecord() {
            return newRecord;
        }

        /**
         *
         * @return
         */
        public Map<String, Object> getOldRecord() {
            return oldRecord;
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
