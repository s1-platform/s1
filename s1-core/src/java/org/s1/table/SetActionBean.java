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

import org.s1.cluster.Locks;
import org.s1.objects.BadDataException;
import org.s1.objects.Objects;
import org.s1.table.errors.ActionBusinessException;
import org.s1.table.errors.AlreadyExistsException;
import org.s1.user.AccessDeniedException;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Table action
 */
public class SetActionBean extends ActionBean{

    public SetActionBean(String name) {
        super(name);
    }

    public Map<String,Object> run(String id, Map<String,Object> record, Map<String,Object> data)
            throws AccessDeniedException, ActionBusinessException, AlreadyExistsException, BadDataException {
        record = Objects.merge(record,data);
        setInternal(id, data);
        return record;
    }

    protected final void setInternal(String id, Map<String, Object> record) throws BadDataException, AlreadyExistsException {
        getTable().check(record);
        //lock table to check unique
        String lockId = null;
        try {
            //lock and set
            //avoiding dead-lock we lock not collection, but some fictional id
            lockId = Locks.lockQuite(getTable().getLockName(Table.TABLE_LOCK_ID), Table.LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
            /*lockId = Locks.lockEntityQuite(new StorageId(getTable().getTableStorage().getDataSource(),
                    getTable().getCollectionId().getDatabase(),
                    getTable().getCollectionId().getCollection(), Table.TABLE_LOCK_ID), Table.LOCK_TIMEOUT, TimeUnit.MILLISECONDS);*/
            getTable().checkUnique(record, false);
            //save
            getTable().collectionSet(id, record);
        } finally {
            Locks.releaseLock(lockId);
        }
    }
}
