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

import org.s1.objects.BadDataException;
import org.s1.objects.Objects;
import org.s1.script.function.ScriptFunctionSet;
import org.s1.table.errors.*;
import org.s1.table.format.FieldsMask;
import org.s1.table.format.Query;
import org.s1.table.format.Sort;
import org.s1.user.AccessDeniedException;

import java.util.List;
import java.util.Map;

/**
 * API for S1 scripting
 */
public class TableScriptFunctions extends ScriptFunctionSet {

    /**
     *
     * @param table
     * @return
     */
    protected Table getTable(String table){
        return Tables.get(table);
    }

    /**
     *
     * @param table
     * @param id
     * @param ctx
     * @return
     * @throws NotFoundException
     * @throws AccessDeniedException
     */
    public Map<String,Object> get(String table, String id, Map<String,Object> ctx) throws NotFoundException, AccessDeniedException {
        return getTable(table).get(id, ctx);
    }

    /**
     *
     * @param table
     * @param list
     * @param search
     * @param sort
     * @param fieldMask
     * @param skip
     * @param max
     * @param ctx
     * @return
     * @throws AccessDeniedException
     */
    public long list(String table, List<Map<String,Object>> list, Map<String,Object> search, Map<String,Object> sort, Map<String,Object> fieldMask, Integer skip, Integer max, Map<String,Object> ctx) throws AccessDeniedException {
        Query q = new Query();
        if(search!=null){
            q.fromMap(search);
        }
        Sort s = new Sort();
        if(sort!=null){
            s.fromMap(sort);
        }
        FieldsMask f = new FieldsMask();
        if(fieldMask!=null){
            f.fromMap(fieldMask);
        }
        return getTable(table).list(list, q, s, f, skip, max, ctx);
    }

    /**
     *
     * @param table
     * @param field
     * @param search
     * @return
     * @throws AccessDeniedException
     */
    public Map<String,Object> aggregate(String table, String field, Map<String,Object> search) throws AccessDeniedException {
        Query q = new Query();
        if(search!=null){
            q.fromMap(search);
        }
        return getTable(table).aggregate(field, q).toMap();
    }

    /**
     *
     * @param table
     * @param field
     * @param search
     * @return
     * @throws AccessDeniedException
     */
    public List<Map<String,Object>> countGroup(String table, String field, Map<String,Object> search) throws AccessDeniedException {
        Query q = new Query();
        if(search!=null){
            q.fromMap(search);
        }
        List<Map<String,Object>> l = Objects.newArrayList();
        List<CountGroupBean> lc = getTable(table).countGroup(field, q);
        for(CountGroupBean c:lc){
            l.add(c.toMap());
        }
        return l;
    }


    public Map<String,Object> add(String table, String action, Map<String,Object> data) throws AlreadyExistsException, AccessDeniedException, ActionBusinessException, BadDataException {
        return getTable(table).add(action, data);
    }

    public Map<String,Object> set(String table, String id, String action, Map<String,Object> data) throws AccessDeniedException, BadDataException, ActionBusinessException, NotFoundException, AlreadyExistsException {
        return getTable(table).set(id, action, data);
    }

    public Map<String,Object> remove(String table, String id, String action, Map<String,Object> data) throws AccessDeniedException, ActionBusinessException, NotFoundException, BadDataException {
        return getTable(table).remove(id, action, data);
    }

    /**
     *
     * @param table
     * @return
     */
    public boolean isAccessAllowed(String table){
        return getTable(table).isAccessAllowed();
    }

    /**
     *
     * @param table
     * @return
     */
    public boolean isImportAccessAllowed(String table){
        return getTable(table).isImportAllowed();
    }
}
