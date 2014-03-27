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
import org.s1.objects.Objects;
import org.s1.objects.schema.ObjectSchema;
import org.s1.objects.schema.SimpleTypeAttribute;
import org.s1.table.ActionBean;
import org.s1.table.IndexBean;
import org.s1.table.Table;
import org.s1.table.TableStorage;

import java.util.List;
import java.util.Map;

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
    public ObjectSchema getSchema() {
        return new ObjectSchema(
                new SimpleTypeAttribute("a","aaa",String.class).setRequired(true),
                new SimpleTypeAttribute("b","bbb",Integer.class)
        );
    }

    @Override
    protected TableStorage createTableStorage() {
        return new MongoDBTableStorage();
    }

    @Override
    public ObjectSchema getImportSchema() {
        return new ObjectSchema(
                new SimpleTypeAttribute("a","imp_aaa",String.class).setRequired(true),
                new SimpleTypeAttribute("b","imp_bbb",Integer.class)
        );
    }

    @Override
    public List<ActionBean> getActions() {
        return Objects.newArrayList(
                new ActionBean("add","Add", ActionBean.Types.ADD, new ObjectSchema(
                        new SimpleTypeAttribute("a","aaa",String.class).setRequired(true),
                        new SimpleTypeAttribute("b","bbb",Integer.class)
                )),
                new ActionBean("setB","Set b", ActionBean.Types.SET, new ObjectSchema(
                        new SimpleTypeAttribute("b1","bbb1",Integer.class)
                )),
                new ActionBean("remove","Remove", ActionBean.Types.REMOVE, null)
        );
    }

    @Override
    protected Map<String, Object> merge(ActionBean action, Map<String, Object> object, Map<String, Object> data) {
        if(action.getName().equals("setB")){
            object.put("b",data.get("b1"));
            return object;
        }else {
            return super.merge(action, object, data);
        }
    }

    /*@Override
    public List<String> getFullTextFields() {
        return Objects.newArrayList("a","b");
    }*/
}
