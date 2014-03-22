/*
 * Copyright 2014 Grigory Pykhov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.s1.table;

import org.s1.S1SystemError;
import org.s1.cluster.Session;
import org.s1.cluster.dds.DDSCluster;
import org.s1.cluster.dds.DistributedDataSource;
import org.s1.cluster.dds.EntityIdBean;
import org.s1.cluster.dds.Transactions;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.ObjectPath;
import org.s1.objects.Objects;
import org.s1.objects.schema.ListAttribute;
import org.s1.objects.schema.MapAttribute;
import org.s1.objects.schema.ObjectSchema;
import org.s1.objects.schema.ObjectSchemaAttribute;
import org.s1.objects.schema.errors.ValidationException;
import org.s1.table.errors.*;
import org.s1.table.format.*;
import org.s1.user.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    protected Class<? extends DistributedDataSource> dataSource;
    protected String database;
    protected String collection;
    protected List<IndexBean> indexes = Objects.newArrayList(IndexBean.class,
            new IndexBean(Objects.newArrayList(String.class, "id"), true, "id"));

    protected ObjectSchema schema;
    protected ObjectSchema importSchema;

    protected List<ActionBean> actions = Objects.newArrayList();

    /**
     * LOCK TIMEOUT
     */
    public static int LOCK_TIMEOUT = 30000;

    public static final String CTX_VALIDATE_KEY = "validate";
    public static final String CTX_DEEP_KEY = "deep";
    public static final String CTX_EXPAND_KEY = "expand";

    /**
     *
     */
    public void init() {
        //indexes
        checkIndexes();
    }


    /*==========================================
     * DESCRIPTOR
     ==========================================*/

    public Class<? extends DistributedDataSource> getDataSource() {
        return dataSource;
    }

    public String getDatabase() {
        return database;
    }

    public String getCollection() {
        return collection;
    }

    public List<IndexBean> getIndexes() {
        return indexes;
    }

    public ObjectSchema getSchema() {
        return schema;
    }

    public ObjectSchema getImportSchema() {
        return importSchema;
    }

    public List<ActionBean> getActions() {
        return actions;
    }

    /*==========================================
     * INDEXES
     ==========================================*/

    protected abstract void collectionIndex(String collection, String name, IndexBean ind);

    public void checkIndexes() {
        int i = 0;
        for (IndexBean b : getIndexes()) {
            collectionIndex(getCollection(), "index_" + i, b);
            i++;
        }
    }

    protected void checkUnique(Map<String, Object> object, boolean isNew) throws AlreadyExistsException {
        //validate unique
        for (IndexBean ind : getIndexes()) {
            if (ind.isUnique()) {
                //check unique
                GroupQueryNode gqn = new GroupQueryNode(GroupQueryNode.GroupOperation.AND);
                Query search = new Query(gqn);
                String err = "";

                int i = 0;
                for (String f : ind.getFields()) {
                    i++;
                    gqn.getChildren().add(new FieldQueryNode(f, FieldQueryNode.FieldOperation.EQUALS, Objects.get(object, f)));
                    err += getAttributeLabel(f);
                    if (i < ind.getFields().size())
                        err += "; ";
                }
                if (!Objects.isNullOrEmpty(ind.getUniqueErrorMessage())) {
                    err = ind.getUniqueErrorMessage();
                }

                String id = Objects.get(object, "id");


                if (!isNew) {
                    FieldQueryNode f = new FieldQueryNode("id", FieldQueryNode.FieldOperation.EQUALS, id);
                    f.setNot(true);
                    gqn.getChildren().add(f);
                }
                try {
                    try {
                        Map<String, Object> m = collectionGet(search);
                    } catch (MoreThanOneFoundException e) {
                    }
                    throw new AlreadyExistsException(err);
                } catch (NotFoundException e) {
                    //ok
                }
            }
        }
    }

    /*==========================================
     * READ
     ==========================================*/

    protected void enrichRecord(Map<String, Object> record, boolean list, Map<String, Object> ctx) {
        if (ctx.containsKey(Table.CTX_VALIDATE_KEY)) {
            boolean expand = Objects.get(Boolean.class, ctx, Table.CTX_EXPAND_KEY, false);
            boolean deep = Objects.get(Boolean.class, ctx, Table.CTX_DEEP_KEY, false);
            Map<String, Object> r = Objects.copy(record);
            try {
                r = getSchema().validate(r, expand, deep, null);
            } catch (ValidationException e) {
                LOG.warn("Cannot validate data on table '" + getName() + "' schema: " + e.getMessage());
            }
            record.clear();
            record.putAll(r);
        }
    }

    protected void prepareSearch(Query search) {

    }

    protected void prepareSort(Sort s) {

    }

    public long list(List<Map<String, Object>> result,
                     Query search, Sort sort, FieldsMask fields,
                     int skip, int max, Map<String, Object> ctx) throws AccessDeniedException {
        if (ctx == null)
            ctx = Objects.newHashMap();
        checkAccess();
        if (search == null)
            search = new Query();
        prepareSearch(search);

        if (sort == null)
            sort = new Sort();
        prepareSort(sort);

        long count = collectionList(result, search, sort, fields, skip, max);
        for (Map<String, Object> m : result) {
            try {
                enrichRecord(m, true, ctx);
            } catch (Exception e) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Error enrich: " + e.getMessage());
            }
        }
        return count;
    }

    public Map<String, Object> get(String id, Map<String, Object> ctx) throws NotFoundException, AccessDeniedException {
        if (ctx == null)
            ctx = Objects.newHashMap();
        checkAccess();
        Query search = new Query(new FieldQueryNode("id", FieldQueryNode.FieldOperation.EQUALS, id));
        prepareSearch(search);
        Map<String, Object> m = null;
        try {
            m = collectionGet(search);
        } catch (MoreThanOneFoundException e) {
            throw S1SystemError.wrap(e);
        }
        try {
            enrichRecord(m, false, ctx);
        } catch (Exception e) {
            if (LOG.isDebugEnabled())
                LOG.warn("Error enrich: " + e.getMessage());
        }
        return m;
    }

    public Map<String, Object> get(String id) throws NotFoundException, AccessDeniedException {
        return get(id, null);
    }

    public long list(List<Map<String, Object>> result, Query search, Sort sort, FieldsMask fields, int skip, int max) throws AccessDeniedException {
        return list(result, search, sort, fields, skip, max, null);
    }

    public AggregationBean aggregate(String field, Query search) throws AccessDeniedException {
        checkAccess();
        if (search == null)
            search = new Query();
        prepareSearch(search);
        return collectionAggregate(field, search);
    }

    public List<CountGroupBean> countGroup(String field, Query search) throws AccessDeniedException {
        checkAccess();
        if (search == null)
            search = new Query();
        prepareSearch(search);
        return collectionCountGroup(field, search);
    }

    protected abstract long collectionList(List<Map<String, Object>> result,
                                           Query search, Sort sort, FieldsMask fields,
                                           int skip, int max);

    protected abstract Map<String, Object> collectionGet(Query search) throws NotFoundException, MoreThanOneFoundException;

    protected abstract AggregationBean collectionAggregate(String field, Query search);

    protected abstract List<CountGroupBean> collectionCountGroup(String field, Query search);

    /*==========================================
     * WRITER
     ==========================================*/

    protected abstract void collectionAdd(String id, Map<String, Object> data);

    protected abstract void collectionSet(String id, Map<String, Object> data);

    protected abstract void collectionRemove(String id);

    public Map<String, Object> changeState(final String id, final String action,
                                           final Map<String, Object> data, final Map<String, Object> foundation)
            throws AccessDeniedException, ValidationException, ActionNotAvailableException, AlreadyExistsException, NotFoundException, CustomActionException {
        checkAccess();
        try {
            if (Objects.isNullOrEmpty(id) || Transactions.isInTransaction()) {
                //add
                return changeRecordState(id, action, data, foundation);
            } else {
                //lock and set
                return (Map<String, Object>) DDSCluster.lockEntity(new EntityIdBean(getDataSource(), getDatabase(), getCollection(), id), new Closure<String, Object>() {
                    @Override
                    public Object call(String input) throws ClosureException {
                        try {
                            return changeRecordState(id, action, data, foundation);
                        } catch (Throwable e) {
                            throw ClosureException.wrap(e);
                        }
                    }
                }, Table.LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
            }
        } catch (TimeoutException e) {
            throw S1SystemError.wrap(e);
        } catch (ClosureException e) {
            if (e.getCause() != null) {
                if (e.getCause() instanceof ValidationException) {
                    throw (ValidationException) e.getCause();
                } else if (e.getCause() instanceof ActionNotAvailableException) {
                    throw (ActionNotAvailableException) e.getCause();
                } else if (e.getCause() instanceof AlreadyExistsException) {
                    throw (AlreadyExistsException) e.getCause();
                } else if (e.getCause() instanceof NotFoundException) {
                    throw (NotFoundException) e.getCause();
                } else if (e.getCause() instanceof CustomActionException) {
                    throw (CustomActionException) e.getCause();
                }
            }
            throw e.toSystemError();
        }
    }

    protected Map<String, Object> changeRecordState(String id, String action,
                                                    Map<String, Object> data, Map<String, Object> foundation)
            throws ValidationException, ActionNotAvailableException, AlreadyExistsException, NotFoundException, AccessDeniedException, CustomActionException {
        if (data == null)
            data = Objects.newHashMap();

        Map<String, Object> oldObject = Objects.newHashMap();
        ActionBean a = getAction(id, action, oldObject);
        final ActionBean.Types type = a.getType();
        if (type == ActionBean.Types.ADD) {
            //add
            oldObject = null;
            if (Objects.isNullOrEmpty(id))
                id = newId();
        }

        //validate data
        if (a.getSchema() != null)
            data = a.getSchema().validate(data);

        //brules
        runBefore(a, oldObject, data, foundation);

        Map<String, Object> newObject = null;
        if (type != ActionBean.Types.REMOVE) {
            newObject = Objects.newHashMap("id", id);
            if (type == ActionBean.Types.SET)
                newObject = Objects.copy(oldObject);

            //merge
            newObject = merge(a, newObject, data);

            if (getSchema() != null)
                newObject = getSchema().validate(newObject);

            newObject.put("id", id);

            final Map<String, Object> _newObject = newObject;
            final String _id = id;
            try {
                if (Transactions.isInTransaction()) {
                    checkUnique(_newObject, type == ActionBean.Types.ADD);
                    //save
                    if (type == ActionBean.Types.SET)
                        collectionSet(_id, _newObject);
                    else
                        collectionAdd(_id, _newObject);
                } else {
                    DDSCluster.lockEntity(new EntityIdBean(getDataSource(), getDatabase(), getCollection(), ""), new Closure<String, Object>() {
                        @Override
                        public Object call(String input) throws ClosureException {
                            try {
                                checkUnique(_newObject, type == ActionBean.Types.ADD);
                                //save
                                if (type == ActionBean.Types.SET)
                                    collectionSet(_id, _newObject);
                                else
                                    collectionAdd(_id, _newObject);
                            } catch (Throwable e) {
                                throw ClosureException.wrap(e);
                            }

                            return null;
                        }
                    }, LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
                }
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
        } else {
            collectionRemove(id);
        }

        runAfter(a, oldObject, newObject, data, foundation);

        return type != ActionBean.Types.REMOVE ? newObject : oldObject;
    }

    protected Map<String, Object> merge(ActionBean action, Map<String, Object> object, Map<String, Object> data) {
        Map<String, Object> result = Objects.merge(Objects.newHashMap(String.class, Object.class), object, data);
        return result;
    }

    protected void runBefore(ActionBean action,
                             Map<String, Object> oldObject,
                             Map<String, Object> data, Map<String, Object> foundation) throws CustomActionException {

    }

    protected void runAfter(ActionBean action,
                            Map<String, Object> oldObject, Map<String, Object> newObject,
                            Map<String, Object> data, Map<String, Object> foundation) throws CustomActionException {

    }

    /*==========================================
     * IMPORT
     ==========================================*/

    public List<Map<String, Object>> doImport(List<Map<String, Object>> list) throws AccessDeniedException {
        List<Map<String, Object>> res = Objects.newArrayList();
        for (Map<String, Object> element : list) {
            try {
                Map<String, Object> oldObject = null;
                String id = Objects.get(element, "id");
                if (id != null) {
                    try {
                        oldObject = get(id);
                    } catch (NotFoundException e) {
                    }
                } else {
                    id = newId();
                    element.put("id", id);
                }
                if (getImportSchema() != null)
                    element = getImportSchema().validate(element, Objects.newHashMap(String.class, Object.class, "record", oldObject));

                final Map<String, Object> _element = element;
                final String _id = id;
                final Map<String, Object> _oldObject = oldObject;
                DDSCluster.lockEntity(new EntityIdBean(getDataSource(), getDatabase(), getCollection(), id), new Closure<String, Object>() {
                    @Override
                    public Object call(String input) throws ClosureException {
                        try {
                            importRecord(_id, _oldObject, _element);
                        } catch (Throwable e) {
                            throw ClosureException.wrap(e);
                        }
                        return null;
                    }
                }, Table.LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
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

    protected void importRecord(final String id, final Map<String, Object> oldObject, final Map<String, Object> data)
            throws ValidationException, AlreadyExistsException {
        Map<String, Object> newObject = mergeImport(id, oldObject, data);
        newObject.put("id", id);

        //validate
        if (getSchema() != null)
            newObject = getSchema().validate(newObject);

        final Map<String, Object> _newObject = newObject;
        try {
            DDSCluster.lockEntity(new EntityIdBean(getDataSource(), getDatabase(), getCollection(), ""), new Closure<String, Object>() {
                @Override
                public Object call(String input) throws ClosureException {
                    try {
                        //unique
                        checkUnique(_newObject, oldObject == null);

                        //save
                        if (oldObject != null) {
                            //set
                            collectionSet(id, _newObject);
                        } else {
                            //add
                            collectionAdd(id, _newObject);
                        }
                    } catch (Throwable e) {
                        throw ClosureException.wrap(e);
                    }
                    return null;
                }
            }, Table.LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
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

    protected Map<String, Object> mergeImport(String id, Map<String, Object> oldObject, Map<String, Object> data) {
        Map<String, Object> newObject = Objects.merge(Objects.newHashMap(String.class, Object.class), oldObject, data);
        return newObject;
    }

    /*==========================================
     * ACCESS CONTROL
     ==========================================*/

    public void checkAccess() throws AccessDeniedException {
        if (!isAccessAllowed())
            throw new AccessDeniedException("Access to " + getName() + " table is denied for: " + Session.getSessionBean().getUserId());
    }

    public void checkImportAccess() throws AccessDeniedException {
        if (!isImportAllowed())
            throw new AccessDeniedException("Import to " + getName() + " table is denied for: " + Session.getSessionBean().getUserId());
    }

    public boolean isAccessAllowed() {
        return true;
    }

    public boolean isImportAllowed() {
        return true;
    }

    public boolean isActionAllowed(ActionBean action, Map<String, Object> record) {
        return true;
    }

    /*==========================================
     * MISC
     ==========================================*/

    public String getName() {
        return new EntityIdBean(getDataSource(), getDatabase(), getCollection(), "").getLockName();
    }

    protected String newId() {
        return getCollection() + "_" + UUID.randomUUID().toString();
    }

    public ActionBean getAction(String id, String name, Map<String, Object> record) throws ActionNotAvailableException, NotFoundException {
        if (record == null)
            record = Objects.newHashMap();
        ActionBean action = null;
        for (ActionBean it : getActions()) {
            if (it.getName().equals(name)) {
                action = it;
                break;
            }
        }
        if (action == null)
            throw new S1SystemError("Action " + name + " not found in table " + getName());

        Map<String, Object> oldObject = null;
        if (action.getType() != ActionBean.Types.ADD) {
            Query search = new Query(new FieldQueryNode("id", FieldQueryNode.FieldOperation.EQUALS, id));
            prepareSearch(search);
            try {
                oldObject = collectionGet(search);
            } catch (MoreThanOneFoundException e) {
                throw S1SystemError.wrap(e);
            }
            record.clear();
            record.putAll(oldObject);
            //get(id);
        }
        List<ActionBean> actions = getAvailableActions(oldObject);
        action = null;
        for (ActionBean it : actions) {
            if (it.getName().equals(name)) {
                action = it;
                break;
            }
        }
        if (action == null)
            throw new ActionNotAvailableException("Action " + name + " is not allowed for "
                    + Session.getSessionBean().getUserId()
                    + " in table " + getName());

        return action;
    }

    public List<ActionBean> getAvailableActions(String id) throws NotFoundException, AccessDeniedException {
        Map<String, Object> obj = null;
        if (id != null) {
            obj = get(id);
        }
        return getAvailableActions(obj);
    }

    public List<ActionBean> getAvailableActions(Map<String, Object> record) {
        List<ActionBean> a = Objects.newArrayList();
        for (ActionBean it : getActions()) {
            //check access
            if (isActionAllowed(it, record))
                a.add(it);
        }
        return a;
    }

    public String getAttributeLabel(String path) {
        String label = "";
        if (getSchema() == null)
            return path;
        try {
            String[] ps = ObjectPath.tokenizePath(path);
            ObjectSchemaAttribute a = getSchema().getRootMapAttribute();
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
                label += a.getLabel();
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

}
