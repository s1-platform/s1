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
import org.s1.cluster.dds.beans.StorageId;
import org.s1.misc.Closure;
import org.s1.objects.BadDataException;
import org.s1.objects.MapMethod;
import org.s1.objects.Objects;
import org.s1.table.errors.*;
import org.s1.user.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Base table class
 */
public abstract class Table {

    private static final Logger LOG = LoggerFactory.getLogger(Table.class);

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

    protected abstract void collectionIndex(String name, IndexBean index);

    public void checkIndexes() {
        int i = 0;
        collectionIndex("index_id", new IndexBean(Objects.newArrayList("id")));
        for (IndexBean b : getIndexes()) {
            collectionIndex("index_" + i, b);
            i++;
        }
    }

    protected abstract void checkUnique(Map<String, Object> object, boolean isNew) throws AlreadyExistsException;

    /*==========================================
     * READ
     ==========================================*/

    protected abstract Map<String,Object> collectionGet(String id) throws NotFoundException, MoreThanOneFoundException;

    protected void enrichRecord(Map<String, Object> record, Map<String, Object> ctx) {

    }

    @MapMethod(names = {"id","ctx"})
    public Map<String, Object> get(String id, Map<String, Object> ctx) throws NotFoundException, AccessDeniedException {
        if (ctx == null)
            ctx = Objects.newHashMap();
        checkAccess();
        Map<String, Object> m = null;
        try {
            m = collectionGet(id);
        } catch (MoreThanOneFoundException e) {
            throw S1SystemError.wrap(e);
        }
        try {
            enrichRecord(m, ctx);
        } catch (Throwable e) {
            if (LOG.isDebugEnabled())
                LOG.warn("Error enrich: " + e.getMessage());
        }
        checkRecordAccess(m);
        return m;
    }

    public Map<String, Object> get(String id) throws NotFoundException, AccessDeniedException {
        return get(id, null);
    }

    /*==========================================
     * WRITER
     ==========================================*/

    protected abstract void collectionAdd(String id, Map<String, Object> data);

    protected abstract void collectionSet(String id, Map<String, Object> data);

    protected abstract void collectionRemove(String id);

    @MapMethod(names = {"action","data"})
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

    @MapMethod(names = {"id","action","data"})
    public Map<String, Object> set(final String id, final String action,
                                           final Map<String, Object> data)
            throws AccessDeniedException, AlreadyExistsException, NotFoundException, ActionBusinessException, BadDataException {
        checkAccess();
        String lockId = null;
        try {
            //lock and set
            //lockId = Locks.lockEntityQuite(new StorageId(getTableStorage().getDataSource(), getCollectionId().getDatabase(), getCollectionId().getCollection(), id),Table.LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
            lockId = Locks.lockQuite(getLockName(id),Table.LOCK_TIMEOUT, TimeUnit.MILLISECONDS);

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

    @MapMethod(names = {"id","action","data"})
    public Map<String, Object> remove(final String id, final String action,
                                   final Map<String, Object> data)
            throws AccessDeniedException, NotFoundException, ActionBusinessException, BadDataException {
        checkAccess();
        String lockId = null;
        try {
            //lock and set
            //lockId = Locks.lockEntityQuite(new StorageId(getTableStorage().getDataSource(), getCollectionId().getDatabase(), getCollectionId().getCollection(), id),Table.LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
            lockId = Locks.lockQuite(getLockName(id),Table.LOCK_TIMEOUT, TimeUnit.MILLISECONDS);

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
     * LOCKS
     ==========================================*/

    protected String getLockName(String id){
        return getName()+"/"+id;
    }

    /*==========================================
     * ACCESS CONTROL
     ==========================================*/

    public void checkAccess() throws AccessDeniedException {
        if (!isAccessAllowed())
            throw new AccessDeniedException("Access to " + getName() + " table is denied for: " + Session.getSessionBean().getUserId());
    }

    public void checkRecordAccess(Map<String,Object> record) throws AccessDeniedException {
        if (!isRecordAccessAllowed(record))
            throw new AccessDeniedException("Access to record #" + Objects.get(record,"id") + " in " + getName() + " table is denied for: " + Session.getSessionBean().getUserId());
    }

    @MapMethod(names = {})
    public boolean isAccessAllowed() {
        return true;
    }

    public boolean isActionAllowed(ActionBean action, Map<String, Object> record) {
        return true;
    }

    public boolean isRecordAccessAllowed(Map<String, Object> record) {
        return true;
    }

    /*==========================================
     * MISC
     ==========================================*/

    public abstract String getName();

}
