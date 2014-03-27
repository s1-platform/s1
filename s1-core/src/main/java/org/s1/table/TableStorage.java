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

import org.s1.cluster.dds.DistributedDataSource;
import org.s1.table.errors.MoreThanOneFoundException;
import org.s1.table.errors.NotFoundException;
import org.s1.table.format.FieldsMask;
import org.s1.table.format.Query;
import org.s1.table.format.Sort;

import java.util.List;
import java.util.Map;

/**
 * Table storage
 */
public abstract class TableStorage {

    protected Table table;

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    protected TableStorage() {
    }

    public abstract Class<? extends DistributedDataSource> getDataSource();

    public abstract void collectionAdd(String id, Map<String, Object> data);

    public abstract void collectionSet(String id, Map<String, Object> data);

    public abstract void collectionRemove(String id);


    public abstract long collectionList(List<Map<String, Object>> result,
                                           Query search, Sort sort, FieldsMask fields,
                                           int skip, int max);

    public abstract Map<String, Object> collectionGet(Query search) throws NotFoundException, MoreThanOneFoundException;

    public abstract AggregationBean collectionAggregate(String field, Query search);

    public abstract List<CountGroupBean> collectionCountGroup(String field, Query search);

    public abstract void collectionIndex(String name, IndexBean ind);

    public abstract void init();


}
