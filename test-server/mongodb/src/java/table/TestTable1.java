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

package table;

import org.s1.cluster.dds.beans.CollectionId;
import org.s1.mongodb.table.MongoDBTableStorage;
import org.s1.objects.BadDataException;
import org.s1.objects.Objects;
import org.s1.table.*;
import org.s1.table.errors.ActionBusinessException;
import org.s1.table.errors.AlreadyExistsException;
import org.s1.user.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.03.2014
 * Time: 12:24
 */
public class TestTable1 extends Table {

    @Override
    public CollectionId getCollectionId() {
        return new CollectionId(null,"coll1");
    }

    @Override
    public List<IndexBean> getIndexes() {
        return Objects.newArrayList(
                new IndexBean(Objects.newArrayList("a"),true,null)
                );
    }

    @Override
    protected TableStorage createTableStorage() {
        return new MongoDBTableStorage();
    }

    @Override
    protected Map<String, Object> mergeRecordBeforeImport(String id, Map<String, Object> oldObject, Map<String, Object> data) throws BadDataException {
        if(Objects.isNullOrEmpty(id))
            id = UUID.randomUUID().toString();
        String a = Objects.get(data,"a");
        int b = Objects.get(Integer.class,data,"b");
        if(Objects.isNullOrEmpty(a))
            throw new BadDataException("imp_aaa");

        Map<String,Object> m = Objects.newSOHashMap(
                "id",id,
                "a",a,
                "b",b
        );
        return m;
    }

    @Override
    public List<AddActionBean> getAddActions() {
        return Objects.newArrayList(AddActionBean.class, new AddActionBean("add"){
            @Override
            public Map<String, Object> run(Map<String, Object> data) throws AccessDeniedException, ActionBusinessException, AlreadyExistsException, BadDataException {
                String id = UUID.randomUUID().toString();
                String a = Objects.get(data,"a");
                int b = Objects.get(Integer.class,data,"b");
                if(Objects.isNullOrEmpty(a))
                    throw new BadDataException("aaa");

                Map<String,Object> m = Objects.newSOHashMap(
                        "a",a,
                        "b",b
                );
                addIternal(id, m);
                return m;
            }
        });
    }

    @Override
    public List<SetActionBean> getSetActions() {
        return Objects.newArrayList(SetActionBean.class, new SetActionBean("setB"){
            @Override
            public Map<String, Object> run(String id, Map<String, Object> record, Map<String, Object> data) throws AccessDeniedException, ActionBusinessException, AlreadyExistsException, BadDataException {
                int b = Objects.get(Integer.class,data,"b1");
                if(Objects.isNullOrEmpty(b))
                    throw new BadDataException("bbb1");

                record.put("b",b);
                setIternal(id,record);
                return record;
            }
        });
    }

    @Override
    public List<RemoveActionBean> getRemoveActions() {
        return Objects.newArrayList(RemoveActionBean.class, new RemoveActionBean("remove"));
    }

    /*@Override
    public List<String> getFullTextFields() {
        return Objects.newArrayList("a","b");
    }*/
}
