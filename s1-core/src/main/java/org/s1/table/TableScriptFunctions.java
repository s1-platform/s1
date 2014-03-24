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
import org.s1.objects.Objects;
import org.s1.objects.schema.errors.ValidationException;
import org.s1.script.functions.ScriptFunctions;
import org.s1.table.errors.ActionNotAvailableException;
import org.s1.table.errors.AlreadyExistsException;
import org.s1.table.errors.CustomActionException;
import org.s1.table.errors.NotFoundException;
import org.s1.table.format.FieldsMask;
import org.s1.table.format.Query;
import org.s1.table.format.Sort;
import org.s1.user.AccessDeniedException;

import java.util.List;
import java.util.Map;

/**
 * API for S1 scripting
 */
public class TableScriptFunctions extends ScriptFunctions{

    /**
     *
     * @param table
     * @return
     */
    protected Table getTable(String table){
        try{
            Table tbl = (Table)Class.forName(table).newInstance();
            tbl.init();
            return tbl;
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
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
    public long list(String table, List<Map<String,Object>> list, Map<String,Object> search, Map<String,Object> sort, Map<String,Object> fieldMask, int skip, int max, Map<String,Object> ctx) throws AccessDeniedException {
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

    /**
     *
     * @param table
     * @param id
     * @param action
     * @param data
     * @param foundation
     * @return
     * @throws ActionNotAvailableException
     * @throws AccessDeniedException
     * @throws ValidationException
     * @throws NotFoundException
     * @throws AlreadyExistsException
     * @throws CustomActionException
     */
    public Map<String,Object> changeState(String table, String id, String action, Map<String,Object> data) throws ActionNotAvailableException, AccessDeniedException, ValidationException, NotFoundException, AlreadyExistsException, CustomActionException {
        return getTable(table).changeState(id,action,data);
    }

    /**
     *
     * @param table
     * @param id
     * @return
     * @throws NotFoundException
     * @throws AccessDeniedException
     */
    public List<Map<String,Object>> getAvailableActions(String table, String id) throws NotFoundException, AccessDeniedException {
        List<Map<String,Object>> l = Objects.newArrayList();
        List<ActionBean> lc = getTable(table).getAvailableActions(id);
        for(ActionBean c:lc){
            l.add(Objects.newHashMap(String.class,Object.class,
                    "name",c.getName(),
                    "label",c.getLabel(),
                    "type",c.getType().toString().toLowerCase(),
                    "schema",c.getSchema().toMap()));
        }
        return l;
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
