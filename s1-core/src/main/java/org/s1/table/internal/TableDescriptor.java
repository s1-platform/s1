package org.s1.table.internal;

import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;
import org.s1.objects.schema.*;
import org.s1.script.S1ScriptEngine;
import org.s1.script.ScriptException;
import org.s1.table.*;
import org.s1.table.format.Query;
import org.s1.table.format.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Table descriptor
 */
class TableDescriptor {

    private static final Logger LOG = LoggerFactory.getLogger(TableDescriptor.class);

    private TableBase table;

    /**
     *
     * @param table
     */
    public TableDescriptor(TableBase table) {
        this.table = table;
    }

    private String name;
    private String collection;
    private List<IndexBean> indexes = Objects.newArrayList(IndexBean.class,
            new IndexBean(Objects.newArrayList(String.class, "id"), true, "id"));
    private Closure<Object, Boolean> access;
    private Closure<Map<String, Object>, Boolean> logAccess;
    private Closure<Object, Boolean> importAccess;
    private Closure<EnrichBean, Object> enrichRecord;
    private Closure<Query, Object> prepareSearch;
    private Closure<Sort, Object> prepareSort;
    private ObjectSchema schema;
    private ObjectSchema importSchema;
    private Closure<ImportBean, Object> importAction;

    private List<ActionBean> actions = Objects.newArrayList();
    private List<StateBean> states = Objects.newArrayList();

    /**
     * @return
     */
    public List<ActionBean> getActions() {
        return actions;
    }

    /**
     * @return
     */
    public List<StateBean> getStates() {
        return states;
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return
     */
    public String getCollection() {
        return collection;
    }

    /**
     * @param collection
     */
    public void setCollection(String collection) {
        this.collection = collection;
    }

    /**
     * @return
     */
    public List<IndexBean> getIndexes() {
        return indexes;
    }

    /**
     * @return
     */
    public ObjectSchema getSchema() {
        return schema;
    }

    /**
     * @param schema
     */
    public void setSchema(ObjectSchema schema) {
        this.schema = schema;
    }

    /**
     * @return
     */
    public ObjectSchema getImportSchema() {
        return importSchema;
    }

    /**
     * @param importSchema
     */
    public void setImportSchema(ObjectSchema importSchema) {
        this.importSchema = importSchema;
    }

    /**
     * @return
     */
    public Closure<Sort, Object> getPrepareSort() {
        return prepareSort;
    }

    /**
     * @param prepareSort
     */
    public void setPrepareSort(Closure<Sort, Object> prepareSort) {
        this.prepareSort = prepareSort;
    }

    /**
     * @return
     */
    public Closure<Query, Object> getPrepareSearch() {
        return prepareSearch;
    }

    /**
     * @param prepareSearch
     */
    public void setPrepareSearch(Closure<Query, Object> prepareSearch) {
        this.prepareSearch = prepareSearch;
    }

    /**
     * @return
     */
    public Closure<EnrichBean, Object> getEnrichRecord() {
        return enrichRecord;
    }

    /**
     * @param enrichRecord
     */
    public void setEnrichRecord(Closure<EnrichBean, Object> enrichRecord) {
        this.enrichRecord = enrichRecord;
    }

    /**
     * @return
     */
    public Closure<Object, Boolean> getImportAccess() {
        return importAccess;
    }

    /**
     * @param importAccess
     */
    public void setImportAccess(Closure<Object, Boolean> importAccess) {
        this.importAccess = importAccess;
    }

    /**
     * @return
     */
    public Closure<Map<String, Object>, Boolean> getLogAccess() {
        return logAccess;
    }

    /**
     * @param logAccess
     */
    public void setLogAccess(Closure<Map<String, Object>, Boolean> logAccess) {
        this.logAccess = logAccess;
    }

    /**
     * @return
     */
    public Closure<Object, Boolean> getAccess() {
        return access;
    }

    /**
     * @param access
     */
    public void setAccess(Closure<Object, Boolean> access) {
        this.access = access;
    }

    /**
     * @return
     */
    public Closure<ImportBean, Object> getImportAction() {
        return importAction;
    }

    /**
     * @param importAction
     */
    public void setImportAction(Closure<ImportBean, Object> importAction) {
        this.importAction = importAction;
    }

    /**
     * @param m
     */
    protected void fromMap(Map<String, Object> m) {
        collection = Objects.get(m,"collection");

        final S1ScriptEngine scriptEngine = new S1ScriptEngine(Table.SCRIPT_ENGINE_PATH);

        //indexes
        indexes = Objects.newArrayList(IndexBean.class,
                new IndexBean(Objects.newArrayList(String.class, "id"), true, "id"));
        List<Map<String,Object>> indList = Objects.get(m,"indexes");
        if(indList!=null){
            for(Map<String,Object> it:indList){
                List<String> fields = Objects.get(it,"fields");
                boolean unique = Objects.get(Boolean.class, it,"unique");
                String msg = Objects.get(it,"message");
                if(!Objects.isNullOrEmpty(fields)){
                    indexes.add(new IndexBean(fields, unique, msg));
                }
            }
        }

        //actions
        actions = Objects.newArrayList();
        List<Map<String,Object>> actList = Objects.get(m,"actions");
        if(actList!=null){
            for(Map<String,Object> it:actList){
                ActionBean a = new ActionBean();
                try {
                    a.fromMap(scriptEngine, it);
                } catch (ObjectSchemaFormatException e) {
                    LOG.warn("Table " + name + ", action " + a.getName() + " schema format problem: " + e.getMessage() + ", " + e.getClass().getName());
                }
                actions.add(a);
            }
        }

        states = Objects.newArrayList();
        List<Map<String,Object>> stList = Objects.get(m,"states");
        if(stList!=null){
            for(Map<String,Object> it:stList){
                StateBean s = new StateBean();
                try {
                    s.fromMap(it);
                } catch (ObjectSchemaFormatException e) {
                    LOG.warn("Table "+name+", state "+s.getName()+" schema format problem: "+e.getMessage()+", "+e.getClass().getName());
                }
                states.add(s);
            }
        }

        Map<String,Object> sm = Objects.get(m,"schema");
        if(!Objects.isNullOrEmpty(sm)){
            ObjectSchema s = new ObjectSchema();
            try {
                s.fromMap(sm);
                this.setSchema(s);
            } catch (ObjectSchemaFormatException e) {
                LOG.warn("Table " + name + " schema format problem: " + e.getMessage() + ", " + e.getClass().getName());
            }
        }

        Map<String,Object> ism = Objects.get(m,"importSchema");
        if(!Objects.isNullOrEmpty(ism)){
            ObjectSchema s = new ObjectSchema();
            try {
                s.fromMap(ism);
                this.setImportSchema(s);
            } catch (ObjectSchemaFormatException e) {
                LOG.warn("Table "+name+" importSchema format problem: "+e.getMessage()+", "+e.getClass().getName());
            }
        }

        final String accessStr = Objects.get(m,"access","").trim();
        if(!Objects.isNullOrEmpty(accessStr)){
            this.setAccess(new Closure<Object, Boolean>() {
                @Override
                public Boolean call(Object input) throws ClosureException {
                    try{
                        return scriptEngine.evalInFunction(Boolean.class, accessStr, null);
                    }catch (ScriptException e){
                        return false;
                    }
                }
            });
        }

        final String importAccessStr = Objects.get(m,"importAccess","").trim();
        if(!Objects.isNullOrEmpty(importAccessStr)){
            this.setImportAccess(new Closure<Object, Boolean>() {
                @Override
                public Boolean call(Object input) throws ClosureException {
                    try {
                        return scriptEngine.evalInFunction(Boolean.class, importAccessStr, null);
                    } catch (ScriptException e) {
                        return false;
                    }
                }
            });
        }

        final String logAccessStr = Objects.get(m,"logAccess","").trim();
        if(!Objects.isNullOrEmpty(logAccessStr)){
            this.setLogAccess(new Closure<Map<String, Object>, Boolean>() {
                @Override
                public Boolean call(Map<String, Object> input) throws ClosureException {
                    try {
                        return scriptEngine.evalInFunction(Boolean.class,accessStr, Objects.newHashMap(String.class, Object.class,
                                "record", input));
                    } catch (ScriptException e) {
                        return false;
                    }
                }
            });
        }

        final String importActionStr = Objects.get(m,"importAction","").trim();
        if(!Objects.isNullOrEmpty(importActionStr)){
            this.setImportAction(new Closure<ImportBean, Object>() {
                @Override
                public Object call(ImportBean input) throws ClosureException {
                    try {
                        scriptEngine.eval(importActionStr, Objects.newHashMap(String.class, Object.class,
                                "newRecord", input.getNewRecord(),
                                "oldRecord", input.getOldRecord(),
                                "id", input.getId(),
                                "state", input.getState(),
                                "data", input.getData()));
                    } catch (ScriptException e) {
                        ClosureException.wrap(e);
                    }
                    return null;
                }
            });
        }

        final String enrichStr = Objects.get(m,"enrichRecord","").trim();
        if(!Objects.isNullOrEmpty(enrichStr)){
            this.setEnrichRecord(new Closure<EnrichBean, Object>() {
                @Override
                public Object call(EnrichBean input) throws ClosureException {
                    try {
                        scriptEngine.eval(enrichStr, Objects.newHashMap(String.class, Object.class,
                                "context", input.getContext(),
                                "record", input.getRecord()));
                    } catch (ScriptException e) {
                        ClosureException.wrap(e);
                    }
                    return null;
                }
            });
        }

        final String filterStr = Objects.get(m,"prepareSearch","").trim();
        if(!Objects.isNullOrEmpty(filterStr)){
            this.setPrepareSearch(new Closure<Query, Object>() {
                @Override
                public Object call(Query input) throws ClosureException {
                    try {
                        Map<String, Object> m = input.toMap();
                        scriptEngine.eval(filterStr, Objects.newHashMap(String.class, Object.class,
                                "search", m));
                        input.fromMap(m);
                    } catch (ScriptException e) {
                        ClosureException.wrap(e);
                    }
                    return null;
                }
            });
        }

        final String sortStr = Objects.get(m,"prepareSort","").trim();
        if(!Objects.isNullOrEmpty(sortStr)){
            this.setPrepareSort(new Closure<Sort, Object>() {
                @Override
                public Object call(Sort input) throws ClosureException {
                    try {
                        Map<String, Object> m = input.toMap();
                        scriptEngine.eval(sortStr, Objects.newHashMap(String.class, Object.class,
                                "sort", m));
                        input.fromMap(m);
                    } catch (ScriptException e) {
                        ClosureException.wrap(e);
                    }
                    return null;
                }
            });
        }
    }
}
