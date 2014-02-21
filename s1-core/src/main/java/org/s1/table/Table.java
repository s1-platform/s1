package org.s1.table;

import org.s1.S1SystemError;
import org.s1.cluster.Locks;
import org.s1.cluster.Session;
import org.s1.cluster.datasource.AlreadyExistsException;
import org.s1.cluster.datasource.MoreThanOneFoundException;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.ObjectDiff;
import org.s1.objects.ObjectPath;
import org.s1.objects.Objects;
import org.s1.objects.schema.*;
import org.s1.script.S1ScriptEngine;
import org.s1.script.ScriptException;
import org.s1.user.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Base table class
 */
public abstract class Table {

    private static final Logger LOG = LoggerFactory.getLogger(Table.class);

    /**
     * LOCK TIMEOUT
     */
    public static int LOCK_TIMEOUT = 30000;

    private String name;
    private String collection;
    private List<IndexBean> indexes = Objects.newArrayList(IndexBean.class,
            new IndexBean(Objects.newArrayList(String.class, "id"), true, "id"));
    private Closure<Object, Boolean> access;
    private Closure<Map<String, Object>, Boolean> logAccess;
    private Closure<Object, Boolean> importAccess;
    private Closure<EnrichBean, Object> enrich;
    private Closure<Map<String, Object>, Object> filter;
    private Closure<Map<String, Object>, Object> sort;
    private ObjectSchema schema;
    private ObjectSchema importSchema;
    private Closure<ImportBean, Object> importAction;

    private List<ActionBean> actions = Objects.newArrayList();
    private List<StateBean> states = Objects.newArrayList();

    public static final String STATE = "_state";
    public static final String VALIDATE_KEY = "validate";
    public static final String DEEP_KEY = "deep";
    public static final String EXPAND_KEY = "expand";

    public static final String HISTORY_SUFFIX = "_history";
    public static final String REMOVED_SUFFIX = "_removed";

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
    public Closure<Map<String, Object>, Object> getSort() {
        return sort;
    }

    /**
     * @param sort
     */
    public void setSort(Closure<Map<String, Object>, Object> sort) {
        this.sort = sort;
    }

    /**
     * @return
     */
    public Closure<Map<String, Object>, Object> getFilter() {
        return filter;
    }

    /**
     * @param filter
     */
    public void setFilter(Closure<Map<String, Object>, Object> filter) {
        this.filter = filter;
    }

    /**
     * @return
     */
    public Closure<EnrichBean, Object> getEnrich() {
        return enrich;
    }

    /**
     * @param enrich
     */
    public void setEnrich(Closure<EnrichBean, Object> enrich) {
        this.enrich = enrich;
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
    public void fromMap(Map<String, Object> m) {
        collection = Objects.get(m,"collection");

        final S1ScriptEngine scriptEngine = new S1ScriptEngine("table.scriptEngine");

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

        final String accessStr = Objects.get(m,"access");
        if(!Objects.isNullOrEmpty(accessStr)){
            this.setAccess(new Closure<Object, Boolean>() {
                @Override
                public Boolean call(Object input) throws ClosureException {
                    try{
                        scriptEngine.eval(accessStr,null);
                        return true;
                    }catch (ScriptException e){
                        return false;
                    }
                }
            });
        }

        final String importAccessStr = Objects.get(m,"importAccess");
        if(!Objects.isNullOrEmpty(importAccessStr)){
            this.setImportAccess(new Closure<Object, Boolean>() {
                @Override
                public Boolean call(Object input) throws ClosureException {
                    try {
                        scriptEngine.eval(importAccessStr, null);
                        return true;
                    } catch (ScriptException e) {
                        return false;
                    }
                }
            });
        }

        final String logAccessStr = Objects.get(m,"logAccess");
        if(!Objects.isNullOrEmpty(logAccessStr)){
            this.setLogAccess(new Closure<Map<String, Object>, Boolean>() {
                @Override
                public Boolean call(Map<String, Object> input) throws ClosureException {
                    try {
                        scriptEngine.eval(accessStr, Objects.newHashMap(String.class, Object.class,
                                "record", input));
                        return true;
                    } catch (ScriptException e) {
                        return false;
                    }
                }
            });
        }

        final String importActionStr = Objects.get(m,"importAction");
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

        final String enrichStr = Objects.get(m,"enrich");
        if(!Objects.isNullOrEmpty(enrichStr)){
            this.setEnrich(new Closure<EnrichBean, Object>() {
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

        final String filterStr = Objects.get(m,"filter");
        if(!Objects.isNullOrEmpty(filterStr)){
            this.setFilter(new Closure<Map<String, Object>, Object>() {
                @Override
                public Object call(Map<String, Object> input) throws ClosureException {
                    try {
                        scriptEngine.eval(filterStr, Objects.newHashMap(String.class, Object.class,
                                "search", input));
                    } catch (ScriptException e) {
                        ClosureException.wrap(e);
                    }
                    return null;
                }
            });
        }

        final String sortStr = Objects.get(m,"sort");
        if(!Objects.isNullOrEmpty(sortStr)){
            this.setSort(new Closure<Map<String, Object>, Object>() {
                @Override
                public Object call(Map<String, Object> input) throws ClosureException {
                    try {
                        scriptEngine.eval(sortStr, Objects.newHashMap(String.class, Object.class,
                                "sort", input));
                    } catch (ScriptException e) {
                        ClosureException.wrap(e);
                    }
                    return null;
                }
            });
        }

    }

    /**
     *
     */
    public void init() {
        //indexes
        checkIndexes();

    }

    /**
     * @return
     */
    public List<IndexBean> getLogIndexes() {
        return Objects.newArrayList(IndexBean.class,
                new IndexBean(Objects.newArrayList(String.class, "date"), false, null),
                new IndexBean(Objects.newArrayList(String.class, "user"), false, null),
                new IndexBean(Objects.newArrayList(String.class, "id"), true, "id"),
                new IndexBean(Objects.newArrayList(String.class, "record"), false, null),
                new IndexBean(Objects.newArrayList(String.class, "action"), false, null)
        );
    }

    /**
     *
     */
    public void checkIndexes() {
        int i = 0;
        for (IndexBean b : indexes) {
            collectionIndex(collection, "index_" + i, b);
            i++;
        }

        i = 0;
        List<IndexBean> l = getLogIndexes();
        for (IndexBean b : l) {
            collectionIndex(collection, "index_" + i, b);
            i++;
        }
    }

    /**
     * @param collection
     * @param name
     * @param ind
     * @return
     */
    protected abstract void collectionIndex(String collection, String name, IndexBean ind);

    /**
     * @param result
     * @param search
     * @param sort
     * @param fields
     * @param skip
     * @param max
     * @return
     */
    public long list(List<Map<String, Object>> result, Map<String, Object> search, Map<String, Object> sort,
                     Map<String, Object> fields, int skip, int max, Map<String, Object> ctx) throws AccessDeniedException {
        if (ctx == null)
            ctx = Objects.newHashMap();
        checkRegistryAccess();
        if (search == null)
            search = Objects.newHashMap();
        search = filter(search);
        if (sort == null)
            sort = Objects.newHashMap();
        sort = formatListSort(sort);
        long count = collectionList(collection, result, search, sort, fields, skip, max);
        for (Map<String, Object> m : result) {
            try {
                enrich(m, true, ctx);
            } catch (Exception e) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Error enrich: " + e.getMessage());
            }
        }
        return count;
    }

    /**
     * @param result
     * @param search
     * @param sort
     * @param fields
     * @param skip
     * @param max
     * @return
     */
    public long list(List<Map<String, Object>> result, Map<String, Object> search, Map<String, Object> sort,
                     Map<String, Object> fields, int skip, int max) throws AccessDeniedException {
        return list(result, search, sort, fields, skip, max, null);
    }

    /**
     * @param collection
     * @param result
     * @param search
     * @param sort
     * @param fields
     * @param skip
     * @param max
     * @return
     */
    protected abstract long collectionList(String collection, List<Map<String, Object>> result, Map<String, Object> search,
                                           Map<String, Object> sort, Map<String, Object> fields, int skip, int max);

    /**
     * @param result
     * @param id
     * @param skip
     * @param max
     * @return
     * @throws NotFoundException
     */
    public long listLog(List<Map<String, Object>> result, String id,
                        int skip, int max) throws NotFoundException, AccessDeniedException {
        checkRegistryAccess();
        Map<String, Object> oldObject = get(id);
        checkLogAccess(oldObject);

        Map<String, Object> search = getFieldEqualsSearch("record", id);
        long count = collectionList(collection + HISTORY_SUFFIX, result, search, Objects.newHashMap(String.class, Object.class,
                "date", -1
        ), null, skip, max);
        return count;
    }

    /**
     * @param id
     * @param ctx
     * @return
     * @throws NotFoundException
     */
    public Map<String, Object> get(String id, Map<String, Object> ctx) throws NotFoundException, AccessDeniedException {
        if (ctx == null)
            ctx = Objects.newHashMap();
        checkRegistryAccess();
        Map<String, Object> search = getFieldEqualsSearch("id",id);
        search = filter(search);
        Map<String, Object> m = null;
        try {
            m = collectionGet(collection, search);
        } catch (MoreThanOneFoundException e) {
            throw S1SystemError.wrap(e);
        }
        try {
            enrich(m, false, ctx);
        } catch (Exception e) {
            if (LOG.isDebugEnabled())
                LOG.warn("Error enrich: " + e.getMessage());
        }
        return m;
    }

    /**
     * @param id
     * @return
     * @throws NotFoundException
     */
    public Map<String, Object> get(String id) throws NotFoundException, AccessDeniedException {
        return get(id, null);
    }

    /**
     *
     * @param id
     * @return
     */
    protected abstract Map<String,Object> getFieldEqualsSearch(String name, String id);

    /**
     * @param collection
     * @param search
     */
    protected abstract Map<String, Object> collectionGet(String collection, Map<String, Object> search) throws NotFoundException, MoreThanOneFoundException;

    /**
     * @param field
     * @param search
     * @return
     */
    public Map<String, Object> aggregate(String field, Map<String, Object> search) throws AccessDeniedException {
        checkRegistryAccess();
        if (search == null)
            search = Objects.newHashMap();
        search = filter(search);
        return collectionAggregate(collection, field, search);
    }

    /**
     * @param collection
     * @param field
     * @param search
     * @return
     */
    protected abstract Map<String, Object> collectionAggregate(String collection, String field, Map<String, Object> search);

    /**
     * @param field
     * @param search
     * @return
     */
    public List<Map<String, Object>> countGroup(String field, Map<String, Object> search) throws AccessDeniedException {
        checkRegistryAccess();
        if (search == null)
            search = Objects.newHashMap();
        search = filter(search);
        return collectionCountGroup(collection, field, search);
    }

    /**
     * @param collection
     * @param field
     * @param search
     * @return
     */
    protected abstract List<Map<String, Object>> collectionCountGroup(String collection, String field, Map<String, Object> search);

    /**
     * @param record
     * @param list
     * @param ctx
     */
    protected void enrich(Map<String, Object> record, boolean list, Map<String, Object> ctx) {
        if (enrich != null) {
            enrich.callQuite(new EnrichBean(record, ctx, list));
            if (ctx.containsKey(VALIDATE_KEY)) {
                boolean expand = Objects.get(Boolean.class, ctx, EXPAND_KEY, false);
                boolean deep = Objects.get(Boolean.class, ctx, DEEP_KEY, false);
                Map<String, Object> r = Objects.copy(record);
                try {
                    r = schema.validate(r, expand, deep, null);
                } catch (ObjectSchemaValidationException e) {
                    LOG.warn("Cannot validate data on table '" + name + "' schema: " + e.getMessage());
                }
                record.clear();
                record.putAll(r);
            }
        }
    }

    /**
     * @param search
     * @return
     */
    protected Map<String, Object> filter(Map<String, Object> search) {
        if (filter != null) {
            filter.callQuite(search);
        }
        return search;
    }

    /**
     * @param s
     * @return
     */
    protected Map<String, Object> formatListSort(Map<String, Object> s) {
        if (sort != null) {
            sort.callQuite(s);
        }
        return s;
    }

    /**
     * @param id
     * @return
     */
    protected String getLockName(String id) {
        return "Table:" + collection + ":" + id;
    }

    /**
     * @param id
     * @param action
     * @param data
     * @param foundation
     * @return
     */
    public Map<String, Object> changeState(final String id, final String action,
                                           final Map<String, Object> data, final Map<String, Object> foundation)
            throws AccessDeniedException, ObjectSchemaValidationException, ActionNotAvailableException, AlreadyExistsException, NotFoundException {
        checkRegistryAccess();
        try {
            return (Map<String, Object>) Locks.waitAndRun(getLockName(id), new Closure<String, Object>() {
                @Override
                public Object call(String input) throws ClosureException {
                    try {
                        return changeRecordState(id, action, data, foundation);
                    } catch (Throwable e) {
                        throw ClosureException.wrap(e);
                    }
                }
            }, LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw S1SystemError.wrap(e);
        } catch (ClosureException e) {
            if (e.getCause() != null) {
                if (e.getCause() instanceof ObjectSchemaValidationException) {
                    throw (ObjectSchemaValidationException) e.getCause();
                } else if (e.getCause() instanceof ActionNotAvailableException) {
                    throw (ActionNotAvailableException) e.getCause();
                } else if (e.getCause() instanceof AlreadyExistsException) {
                    throw (AlreadyExistsException) e.getCause();
                } else if (e.getCause() instanceof NotFoundException) {
                    throw (NotFoundException) e.getCause();
                }
            }
            throw e.toSystemError();
        }
    }

    /**
     * @param id
     * @param action
     * @param data
     * @param foundation
     * @return
     */
    protected Map<String, Object> changeRecordState(String id, String action,
                                                    Map<String, Object> data, Map<String, Object> foundation)
            throws ObjectSchemaValidationException, ActionNotAvailableException, AlreadyExistsException, NotFoundException, ClosureException {
        if (data == null)
            data = Objects.newHashMap();

        Map<String, Object> oldObject = Objects.newHashMap();
        ActionBean a = getAction(id, action, oldObject);

        //state
        StateBean state = getStateByName(a.getTo());

        //validate data
        if (a.getSchema() != null)
            data = a.getSchema().validate(data, Objects.newHashMap(String.class, Object.class, "record", oldObject));

        //validate foundation
        if (a.isLog() && !Objects.isNullOrEmpty(a.getFrom())) {
            if (foundation == null)
                foundation = Objects.newHashMap();
            //validate foundation
            if (a.getFoundationSchema() != null)
                foundation = a.getFoundationSchema().validate(foundation, Objects.newHashMap(String.class, Object.class,
                        "record", oldObject,
                        "data", data));
        }

        //brules
        runBefore(a, oldObject, data, foundation);

        Map<String, Object> newObject = null;

        //set or add
        if (!Objects.isNullOrEmpty(a.getTo())) {
            Map<String, Object> object = Objects.copy(oldObject);
            if (Objects.isNullOrEmpty(object))
                object = Objects.newHashMap("id", newId());

            //merge
            newObject = merge(a, object, data);
            //validate
            if (state.getSchema() != null)
                newObject = state.getSchema().validate(newObject);
            newObject = schema.validate(newObject);

            newObject.put(STATE, state.getName());

            final Map<String, Object> _newObject = newObject;
            final ActionBean _a = a;
            final String _id = id;
            try {
                Locks.waitAndRun(getLockName(null), new Closure<String, Object>() {
                    @Override
                    public Object call(String input) throws ClosureException {
                        try {
                            checkUnique(_newObject, Objects.isNullOrEmpty(_a.getFrom()));
                            //save
                            if (!Objects.isNullOrEmpty(_a.getFrom())) {
                                //set
                                collectionSet(collection, _id, _newObject);
                            } else {
                                //add
                                collectionAdd(collection, _newObject);
                            }
                        } catch (Throwable e) {
                            throw ClosureException.wrap(e);
                        }
                        return null;
                    }
                }, LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                throw S1SystemError.wrap(e);
            } catch (ClosureException e) {
                if (e.getCause() != null) {
                    if (e.getCause() instanceof AlreadyExistsException) {
                        throw (AlreadyExistsException) e.getCause();
                    }
                }
                throw e.toSystemError();
            }

            //add log
            if (!Objects.isNullOrEmpty(a.getFrom()) && a.isLog()) {
                //log
                log(id, a, oldObject, newObject, data, foundation);
            }
        } else {
            //remove
            collectionRemove(collection, id);
            collectionAdd(collection + REMOVED_SUFFIX, oldObject);
        }
        //brules after
        runAfter(a, oldObject, newObject, data, foundation);
        return newObject != null ? newObject : oldObject;
    }

    /**
     * @param action
     * @param object
     * @param data
     * @return
     */
    protected Map<String, Object> merge(ActionBean action, Map<String, Object> object, Map<String, Object> data) {
        Map<String, Object> result = Objects.merge(Objects.newHashMap(String.class, Object.class), object, data);
        if (action.getMerge() != null) {
            action.getMerge().callQuite(new ActionBean.MergeBean(result, object, data));
        }
        return result;
    }

    /**
     * @param collection
     * @param data
     * @return
     */
    protected abstract void collectionAdd(String collection, Map<String, Object> data);

    /**
     * @param collection
     * @param id
     * @param data
     * @return
     */
    protected abstract void collectionSet(String collection, String id, Map<String, Object> data);

    /**
     * @param collection
     * @param id
     * @return
     */
    protected abstract void collectionRemove(String collection, String id);

    /**
     * @param path
     * @return
     */
    protected String getAttributeLabel(String path) {
        String label = "";
        try {
            String[] ps = ObjectPath.tokenizePath(path);
            ObjectSchemaAttribute a = schema.getRootMapAttribute();
            int i = 0;
            for (String p : ps) {
                i++;
                String l = ObjectPath.getLocalName(p);
                int[] j = ObjectPath.getNumber(p);
                for (ObjectSchemaAttribute _a : ((MapAttribute) a).getAttributes()) {
                    if (_a.getName().equals(l)) {
                        a = _a;
                        break;
                    }
                }
                label += a.getName();
                if (j != null && j.length > 0) {
                    for (int k : j) {
                        a = ((ListAttribute) a).getList().get(k);
                        label += "[" + k + "]";
                    }
                }
                if (i < ps.length)
                    label += " / ";
            }
        } catch (Throwable e) {
            label = path;
        }
        return label;
    }

    /**
     * @param id
     * @param oldObject
     * @param newObject
     * @param foundation
     * @param method
     */
    protected void log(String id, ActionBean action, Map<String, Object> oldObject, Map<String, Object> newObject,
                       Map<String, Object> data, Map<String, Object> foundation) {
        List<ObjectDiff.DiffBean> diff = Objects.diff(oldObject, newObject);
        List<Map<String, Object>> changes = Objects.newArrayList();
        Map<String, Object> h = Objects.newHashMap(
                "action", Objects.newHashMap(
                "from", action.getFrom(),
                "to", action.getTo(),
                "name", action.getName(),
                "label", action.getLabel(),
                "schema", action.getSchema() == null ? null : action.getSchema().toMap(),
                "foundationSchema", action.getFoundationSchema() == null ? null : action.getFoundationSchema().toMap()
        ),
                "id", UUID.randomUUID().toString(),
                "record", id,
                "date", new Date(),
                "user", Session.getSessionBean().getUserId(),
                "foundation", foundation,
                "new", newObject,
                "old", oldObject,
                "changes", changes,
                "data", data
        );
        for (ObjectDiff.DiffBean b : diff) {
            changes.add(Objects.newHashMap(String.class, Object.class,
                    "label", getAttributeLabel(b.getPath()),
                    "new", b.getOldValue(),
                    "old", b.getOldValue(),
                    "path", b.getPath()
            ));
        }
        collectionAdd(collection + HISTORY_SUFFIX, h);
    }

    /**
     * Get new id field value
     *
     * @param data
     * @return
     */
    protected String newId() {
        return collection + "_" + UUID.randomUUID().toString();
    }

    /**
     * @return
     */
    public boolean isRegistryAccessAllowed() {
        if (access != null) {
            return access.callQuite(null);
        }
        return true;
    }


    /**
     * @throws AccessDeniedException
     */
    protected void checkRegistryAccess() throws AccessDeniedException {
        if (!isRegistryAccessAllowed())
            throw new AccessDeniedException("Access to " + name + " table is denied for: " + Session.getSessionBean().getUserId());
    }

    /**
     * @return
     */
    public boolean isImportAllowed() {
        if (importAccess != null) {
            return importAccess.callQuite(null);
        }
        return true;
    }

    /**
     * @throws AccessDeniedException
     */
    protected void checkImportAccess() throws AccessDeniedException {
        if (!isImportAllowed())
            throw new AccessDeniedException("Import to " + name + " table is denied for: " + Session.getSessionBean().getUserId());
    }

    /**
     * @param record
     * @return
     */
    public boolean isLogAccessAllowed(Map<String, Object> record) {
        if (logAccess != null) {
            return logAccess.callQuite(record);
        }
        return true;
    }

    /**
     * @param record
     * @throws AccessDeniedException
     */
    protected void checkLogAccess(Map<String, Object> record) throws AccessDeniedException {
        if (!isLogAccessAllowed(record))
            throw new AccessDeniedException("Access to logs " + name + " table, #" + Objects.get(record, "id") + " is denied for: " + Session.getSessionBean().getUserId());
    }

    /**
     * @param action
     * @param oldObject
     * @param data
     * @param foundation
     * @return
     * @throws AppException
     */
    protected void runBefore(ActionBean action,
                             Map<String, Object> oldObject,
                             Map<String, Object> data, Map<String, Object> foundation) throws ClosureException {
        if (action.getBefore() != null) {
            action.getBefore().call(new ActionBean.BeforeBean(data, foundation, action, oldObject));
        }
    }

    /**
     * @param action
     * @param oldObject
     * @param newObject
     * @param data
     * @param foundation
     * @return
     * @throws AppException
     */
    protected void runAfter(ActionBean action,
                            Map<String, Object> oldObject, Map<String, Object> newObject,
                            Map<String, Object> data, Map<String, Object> foundation) throws ClosureException {
        if (action.getAfter() != null) {
            action.getAfter().call(new ActionBean.AfterBean(data, foundation, action, oldObject, newObject));
        }
    }

    /**
     * @param object
     * @throws AppException
     */
    protected void checkUnique(Map<String, Object> object, boolean isNew) throws AlreadyExistsException {
        //validate unique
        for (IndexBean ind : indexes) {
            if (ind.isUnique()) {
                //check unique
                Map<String, Object> search = Objects.newHashMap();
                String err = ind.getUniqueErrorMessage();
                if (Objects.isNullOrEmpty(err)) {
                    int i = 0;
                    for (String f : ind.getFields()) {
                        i++;
                        search.put(f, Objects.get(object, f));
                        err += getAttributeLabel(f);
                        if (i < ind.getFields().size())
                            err += "; ";
                    }
                }

                String id = Objects.get(object, "id");
                search = getUniqueSearch(id, search);
                /*Objects.newHashMap("$and",search);
                if(!isNew){
                    search["\$and"].add([id:["\$ne":object.id]]);
                }*/
                try {
                    try {
                        collectionGet(collection, search);
                    } catch (MoreThanOneFoundException e) {
                    }
                    throw new AlreadyExistsException(err);
                } catch (NotFoundException e) {
                    //ok
                }
            }
        }
    }

    /**
     * @param id
     * @param pathsAndValues
     * @return
     */
    protected abstract Map<String, Object> getUniqueSearch(String id, Map<String, Object> pathsAndValues);

    /**
     * @param id
     * @return
     */
    public List<ActionBean> getAvailableActions(String id) throws NotFoundException, AccessDeniedException {
        Map<String, Object> obj = null;
        if (id != null) {
            obj = get(id);
        }
        return getAvailableActionsForObject(obj);
    }

    /**
     * @param obj
     * @return
     */
    protected List<ActionBean> getAvailableActionsForObject(Map<String, Object> obj) {
        String state = null;
        if (obj != null) {
            state = Objects.get(obj, STATE);
        }
        List<ActionBean> a = Objects.newArrayList();
        for (ActionBean it : actions) {
            if ((!Objects.isNullOrEmpty(state) && Objects.equals(state, it.getFrom()))
                    || (Objects.isNullOrEmpty(state) && Objects.isNullOrEmpty(it.getFrom()))) {
                //check access
                if (checkSRule(it, obj))
                    a.add(it);
            }
        }
        return a;
    }

    /**
     * @param id
     * @param action
     * @return
     */
    public ActionBean getAction(String id, String action, Map<String, Object> obj) throws ActionNotAvailableException, NotFoundException {
        ActionBean a = null;
        for (ActionBean it : actions) {
            if (it.getName().equals(action)) {
                a = it;
                break;
            }
        }
        if (a == null)
            throw new S1SystemError("Action " + action + " not found in table " + name);

        Map<String, Object> oldObject = null;
        if (!Objects.isNullOrEmpty(a.getFrom())) {
            Map<String, Object> search = getFieldEqualsSearch("id",id);
            filter(search);
            try {
                oldObject = collectionGet(collection, search);
            } catch (MoreThanOneFoundException e) {
                throw S1SystemError.wrap(e);
            }
            obj.clear();
            obj.putAll(oldObject);
            //get(id);
        }
        List<ActionBean> actions = getAvailableActionsForObject(oldObject);
        a = null;
        for (ActionBean it : actions) {
            if (it.getName().equals(action)) {
                a = it;
                break;
            }
        }
        if (a == null)
            throw new ActionNotAvailableException("Action " + action + " is not allowed for " + Session.getSessionBean().getUserId() + " in table " + name);

        return a;
    }

    /**
     * @param s
     * @return
     */
    public StateBean getStateByName(String s) {
        if (s == null)
            return null;
        StateBean state = null;
        for (StateBean it : states) {
            if (s.equals(it.getName())) {
                state = it;
                break;
            }
        }
        if (state == null)
            throw new S1SystemError("State " + s + " not found in table " + name);
        return state;
    }

    /**
     * @param action
     * @param record
     * @return
     */
    protected boolean checkSRule(ActionBean action, Map<String, Object> record) {
        if (action.getAccess() != null) {
            return action.getAccess().callQuite(record);
        }
        return true;
    }

    /**
     * @param list
     * @return
     */
    public List<Map<String, Object>> doImport(List<Map<String, Object>> list) throws AccessDeniedException {
        checkImportAccess();
        List<Map<String, Object>> res = Objects.newArrayList();
        if (importAction == null)
            throw new IllegalArgumentException("Import action undefined for table " + name);
        for (Map<String, Object> element : list) {
            try {
                Map<String, Object> oldObject = null;
                String state = null;
                String id = Objects.get(element, "id");
                if (id != null) {
                    try {
                        oldObject = get(id);
                        state = Objects.get(oldObject, STATE);
                    } catch (NotFoundException e) {
                    }
                } else {
                    id = newId();
                    element.put("id", id);
                }
                if (importSchema != null)
                    element = importSchema.validate(element, Objects.newHashMap(String.class, Object.class, "record", oldObject));
                final Map<String, Object> _element = element;
                final String _id = id;
                final String _state = state;
                final Map<String, Object> _oldObject = oldObject;
                Locks.waitAndRun(getLockName(id), new Closure<String, Object>() {
                    @Override
                    public Object call(String input) throws ClosureException {
                        try {
                            importRecord(_id, _oldObject, _state, _element);
                        } catch (Throwable e) {
                            throw ClosureException.wrap(e);
                        }
                        return null;
                    }
                }, LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
                res.add(Objects.newHashMap(String.class, Object.class, "success", true, "id", id));
            } catch (Throwable e) {
                if (e instanceof ClosureException && e.getCause() != null)
                    e = e.getCause();
                LOG.info("Import error: " + e.getMessage());
                LOG.debug("Import error", e);
                res.add(Objects.newHashMap(String.class, Object.class, "success", false, "message", e.getMessage(), "class", e.getClass().getName()));
            }
        }
        return res;
    }

    /**
     * @param id
     * @param oldObject
     * @param state
     * @param data
     */
    protected void importRecord(final String id, final Map<String, Object> oldObject, final String state, final Map<String, Object> data)
            throws ObjectSchemaValidationException, AlreadyExistsException {
        Map<String, Object> newObject = Objects.newHashMap("id", id);
        importAction.callQuite(new ImportBean(id, newObject, oldObject, state, data));

        if (!newObject.containsKey(STATE)) {
            throw new IllegalStateException("New object must contain _state field after import action (table: " + name + ")");
        }
        StateBean st = getStateByName(Objects.get(String.class, newObject, STATE));
        //validate state
        if (st.getSchema() != null)
            newObject = st.getSchema().validate(newObject);
        //validate
        newObject = schema.validate(newObject);

        final Map<String, Object> _newObject = newObject;
        try {
            Locks.waitAndRun(getLockName(null), new Closure<String, Object>() {
                @Override
                public Object call(String input) throws ClosureException {
                    try {
                        //unique
                        checkUnique(_newObject, oldObject == null);

                        //save
                        if (oldObject != null) {
                            //set
                            collectionSet(collection, id, _newObject);
                        } else {
                            //add
                            collectionAdd(collection, _newObject);
                        }
                    } catch (Throwable e) {
                        throw ClosureException.wrap(e);
                    }
                    return null;
                }
            }, LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw S1SystemError.wrap(e);
        } catch (ClosureException e) {
            if (e.getCause() != null) {
                if (e.getCause() instanceof AlreadyExistsException) {
                    throw (AlreadyExistsException) e.getCause();
                }
            }
            throw e.toSystemError();
        }
    }
}
