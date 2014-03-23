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

import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.s1.table.AggregationBean;
import org.s1.table.CountGroupBean;
import org.s1.table.IndexBean;
import org.s1.table.Table;
import org.s1.table.errors.MoreThanOneFoundException;
import org.s1.table.errors.NotFoundException;
import org.s1.table.format.FieldsMask;
import org.s1.table.format.Query;
import org.s1.table.format.Sort;

import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 23.03.2014
 * Time: 12:31
 */
public class TestTable1 extends Table {

    @Override
    protected void collectionIndex(String name, IndexBean ind) {
        //nop
    }

    private List<Map<String,Object>> l = Objects.newArrayList();

    @Override
    protected long collectionList(List<Map<String, Object>> result, Query search, Sort sort, FieldsMask fields, int skip, int max) {
        result.addAll(l);
        return l.size();
    }

    @Override
    protected Map<String, Object> collectionGet(Query search) throws NotFoundException, MoreThanOneFoundException {
        if(l.size()==0)
            throw new NotFoundException();
        return l.get(0);
    }

    @Override
    protected AggregationBean collectionAggregate(String field, Query search) {
        return new AggregationBean();
    }

    @Override
    protected List<CountGroupBean> collectionCountGroup(String field, Query search) {
        return Objects.newArrayList();
    }

    @Override
    protected void collectionAdd(String id, Map<String, Object> data) {
        l.add(data);
    }

    @Override
    protected void collectionSet(final String id, Map<String, Object> data) {
        Map<String,Object> m = Objects.find(l,new Closure<Map<String, Object>, Boolean>() {
            @Override
            public Boolean call(Map<String, Object> input) {
                return id.equals(input.get("id"));
            }
        });
        if(m!=null) {
            l.set(l.indexOf(m), data);
        }
    }

    @Override
    protected void collectionRemove(final String id) {
        Map<String,Object> m = Objects.find(l,new Closure<Map<String, Object>, Boolean>() {
            @Override
            public Boolean call(Map<String, Object> input) {
                return id.equals(input.get("id"));
            }
        });
        if(m!=null) {
            l.remove(l.indexOf(m));
        }
    }
}
