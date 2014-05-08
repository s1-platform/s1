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
import org.s1.cluster.Locks;
import org.s1.cluster.Session;
import org.s1.cluster.dds.beans.CollectionId;
import org.s1.cluster.dds.beans.StorageId;
import org.s1.misc.Closure;
import org.s1.objects.BadDataException;
import org.s1.objects.Objects;
import org.s1.objects.Ranges;
import org.s1.table.errors.*;
import org.s1.table.format.*;
import org.s1.user.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Base table class
 */
public abstract class Table {

    private static final Logger LOG = LoggerFactory.getLogger(Table.class);

    private TableStorage tableStorage;

    /**
     * LOCK TIMEOUT
     */
    public static int LOCK_TIMEOUT = 30000;

    /**
     * TABLE LOCK ID
     */
    public static final String TABLE_LOCK_ID = "fictional_lock_id";

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

    protected TableStorage getTableStorage() {
        if(tableStorage==null){
            synchronized (this){
                if(tableStorage==null){
                    tableStorage = createTableStorage();
                    tableStorage.setTable(this);
                    tableStorage.init();
                }
            }
        }
        return tableStorage;
    }

    protected abstract TableStorage createTableStorage();

    public abstract CollectionId getCollectionId();

    public abstract List<IndexBean> getIndexes();

    public void check(Map<String,Object> record) throws BadDataException {
        if(Objects.isNullOrEmpty(record.get("id")))
            throw new BadDataException("id");
    }

    public abstract List<AddActionBean> getAddActions();
    public abstract List<SetActionBean> getSetActions();
    public abstract List<RemoveActionBean> getRemoveActions();

    /*==========================================
     * INDEXES
     ==========================================*/

    public void checkIndexes() {
        int i = 0;
        getTableStorage().collectionIndex("index_id", new IndexBean(Objects.newArrayList("id"), true, null));
        for (IndexBean b : getIndexes()) {
            getTableStorage().collectionIndex("index_" + i, b);
            i++;
        }
    }

    protected void checkUnique(Map<String, Object> object, boolean isNew) throws AlreadyExistsException {
        //validate unique
        List<IndexBean> indexBeans = new ArrayList<IndexBean>();
        indexBeans.addAll(getIndexes());
        indexBeans.add(new IndexBean(Objects.newArrayList("id"),true,null));
        for (IndexBean ind : indexBeans) {
            if (ind.isUnique()) {
                //check unique
                GroupQueryNode gqn = new GroupQueryNode(GroupQueryNode.GroupOperation.AND);
                Query search = new Query(gqn);
                String err = "";

                int i = 0;
                for (String f : ind.getFields()) {
                    i++;
                    gqn.getChildren().add(new FieldQueryNode(f, FieldQueryNode.FieldOperation.EQUALS, Objects.get(object, f)));
                    err += f;
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
                        Map<String, Object> m = getTableStorage().collectionGet(search);
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

    }

    protected void prepareSearch(Query search) {

    }

    protected void prepareSort(Sort s) {

    }

    public long count(Query search) throws AccessDeniedException {
        checkAccess();
        if (search == null)
            search = new Query();
        prepareSearch(search);

        long count = getTableStorage().collectionCount(search);
        return count;
    }

    public List<Map<String, Object>> list(Query search, Sort sort, FieldsMask fields,
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

        List<Map<String, Object>> result = getTableStorage().collectionList(search, sort, fields, skip, max);
        for (Map<String, Object> m : result) {
            try {
                enrichRecord(m, true, ctx);
            } catch (Throwable e) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Error enrich: " + e.getMessage());
            }
        }
        return result;
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

        result.addAll(getTableStorage().collectionList(search, sort, fields, skip, max));
        long count = getTableStorage().collectionCount(search);
        for (Map<String, Object> m : result) {
            try {
                enrichRecord(m, true, ctx);
            } catch (Throwable e) {
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
            m = getTableStorage().collectionGet(search);
        } catch (MoreThanOneFoundException e) {
            throw S1SystemError.wrap(e);
        }
        try {
            enrichRecord(m, false, ctx);
        } catch (Throwable e) {
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

    public List<Map<String, Object>> list(Query search, Sort sort, FieldsMask fields, int skip, int max) throws AccessDeniedException {
        return list(search, sort, fields, skip, max, null);
    }

    /*==========================================
     * WRITER
     ==========================================*/

    public Map<String, Object> add(final String action, final Map<String, Object> data)
            throws AccessDeniedException, BadDataException, AlreadyExistsException, ActionBusinessException {
        checkAccess();

        AddActionBean a = Objects.find(getAddActions(),new Closure<AddActionBean, Boolean>() {
            @Override
            public Boolean call(AddActionBean input) {
                return input.getName().equals(action);
            }
        });
        if(a==null)
            throw new S1SystemError("Add action " + action+" not found");
        a.setTable(this);

        return a.run(data);
    }

    public Map<String, Object> set(final String id, final String action,
                                           final Map<String, Object> data)
            throws AccessDeniedException, AlreadyExistsException, NotFoundException, ActionBusinessException, BadDataException {
        checkAccess();
        String lockId = null;
        try {
            //lock and set
            lockId = Locks.lockEntityQuite(new StorageId(getTableStorage().getDataSource(), getCollectionId().getDatabase(), getCollectionId().getCollection(), id),Table.LOCK_TIMEOUT, TimeUnit.MILLISECONDS);

            SetActionBean a = Objects.find(getSetActions(),new Closure<SetActionBean, Boolean>() {
                @Override
                public Boolean call(SetActionBean input) {
                    return input.getName().equals(action);
                }
            });
            if(a==null)
                throw new S1SystemError("Set action " + action+" not found");
            a.setTable(this);

            Map<String,Object> record = get(id);
            return a.run(id, record, data);
        } finally {
            Locks.releaseLock(lockId);
        }
    }

    public Map<String, Object> remove(final String id, final String action,
                                   final Map<String, Object> data)
            throws AccessDeniedException, NotFoundException, ActionBusinessException, BadDataException {
        checkAccess();
        String lockId = null;
        try {
            //lock and set
            lockId = Locks.lockEntityQuite(new StorageId(getTableStorage().getDataSource(), getCollectionId().getDatabase(), getCollectionId().getCollection(), id),Table.LOCK_TIMEOUT, TimeUnit.MILLISECONDS);

            RemoveActionBean a = Objects.find(getRemoveActions(),new Closure<RemoveActionBean, Boolean>() {
                @Override
                public Boolean call(RemoveActionBean input) {
                    return input.getName().equals(action);
                }
            });
            if(a==null)
                throw new S1SystemError("Remove action " + action+" not found");
            a.setTable(this);

            Map<String,Object> record = get(id);
            a.run(id, record, data);
            return record;
        } finally {
            Locks.releaseLock(lockId);
        }
    }

    /*==========================================
     * IMPORT
     ==========================================*/

    public List<ImportResultBean> doImport(List<Map<String, Object>> list) throws AccessDeniedException {
        List<ImportResultBean> res = Objects.newArrayList();
        for (Map<String, Object> element : list) {
            try {
                Map<String, Object> oldObject = null;
                String id = Objects.get(element, "id");
                String lockId = null;
                try {
                    //lock and set
                    lockId = Locks.lockEntityQuite(new StorageId(getTableStorage().getDataSource(), getCollectionId().getDatabase(), getCollectionId().getCollection(), id),Table.LOCK_TIMEOUT, TimeUnit.MILLISECONDS);

                    if (!Objects.isNullOrEmpty(id)) {
                        try {
                            oldObject = get(id);
                        } catch (NotFoundException e) {
                        }
                    }

                    importRecord(id, oldObject, element);
                } finally {
                    Locks.releaseLock(lockId);
                }

                res.add(new ImportResultBean(id));
            } catch (Throwable e) {
                LOG.info("Import error: " + e.getMessage());
                LOG.debug("Import error", e);
                res.add(new ImportResultBean(e.getClass().getName(),e.getMessage()));
            }
        }
        return res;
    }

    protected void importRecord(String id, final Map<String, Object> oldObject, final Map<String, Object> data)
            throws BadDataException, AlreadyExistsException {
        Map<String, Object> newObject = mergeRecordBeforeImport(id, oldObject, data);

        //validate
        check(newObject);
        id = Objects.get(newObject,"id");

        //lock table to check unique
        String lockId = null;
        try {
            //lock and set
            lockId = Locks.lockEntityQuite(new StorageId(getTableStorage().getDataSource(), getCollectionId().getDatabase(), getCollectionId().getCollection(), TABLE_LOCK_ID),Table.LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
            checkUnique(newObject, oldObject==null);
            //save
            if (oldObject!=null)
                getTableStorage().collectionSet(id, newObject);
            else
                getTableStorage().collectionAdd(id, newObject);
        } finally {
            Locks.releaseLock(lockId);
        }
    }

    protected Map<String, Object> mergeRecordBeforeImport(String id, Map<String, Object> oldObject, Map<String, Object> data)
        throws BadDataException {
        Map<String, Object> newObject = Objects.merge(oldObject, data);
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
        return new StorageId(getTableStorage().getDataSource(), getCollectionId().getDatabase(), getCollectionId().getCollection(), "").getLockName();
    }

}
