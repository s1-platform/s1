package org.s1.table;

import org.s1.cluster.datasource.AlreadyExistsException;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.objects.Objects;
import org.s1.objects.schema.ObjectSchemaValidationException;
import org.s1.script.functions.ScriptFunctions;
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

    public Map<String,Object> get(String table, String id, Map<String,Object> ctx) throws NotFoundException, AccessDeniedException {
        return TablesFactory.getTable(table).get(id,ctx);
    }

    public long list(String table, List<Map<String,Object>> list, String fullText, Map<String,Object> search, Map<String,Object> sort, Map<String,Object> fieldMask, int skip, int max, Map<String,Object> ctx) throws AccessDeniedException {
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
        return TablesFactory.getTable(table).list(list,fullText,q,s,f,skip,max,ctx);
    }

    public long listLog(String table, List<Map<String,Object>> list, String id, Map<String,Object> search, int skip, int max) throws NotFoundException, AccessDeniedException {
        Query q = new Query();
        if(search!=null){
            q.fromMap(search);
        }
        return TablesFactory.getTable(table).listLog(list, id, q, skip, max);
    }

    public Map<String,Object> aggregate(String table, String field, Map<String,Object> search) throws AccessDeniedException {
        Query q = new Query();
        if(search!=null){
            q.fromMap(search);
        }
        return TablesFactory.getTable(table).aggregate(field, q).toMap();
    }

    public List<Map<String,Object>> countGroup(String table, String field, Map<String,Object> search) throws AccessDeniedException {
        Query q = new Query();
        if(search!=null){
            q.fromMap(search);
        }
        List<Map<String,Object>> l = Objects.newArrayList();
        List<CountGroupBean> lc = TablesFactory.getTable(table).countGroup(field, q);
        for(CountGroupBean c:lc){
            l.add(c.toMap());
        }
        return l;
    }

    public Map<String,Object> changeState(String table, String id, String action, Map<String,Object> data, Map<String,Object> foundation) throws ActionNotAvailableException, AccessDeniedException, ObjectSchemaValidationException, NotFoundException, AlreadyExistsException {
        return TablesFactory.getTable(table).changeState(id,action,data,foundation);
    }

    public List<Map<String,Object>> getAvailableActions(String table, String id) throws NotFoundException, AccessDeniedException {
        List<Map<String,Object>> l = Objects.newArrayList();
        List<ActionBean> lc = TablesFactory.getTable(table).getAvailableActions(id);
        for(ActionBean c:lc){
            l.add(c.toMap());
        }
        return l;
    }

    public List<Map<String,Object>> getStates(String table){
        List<Map<String,Object>> l = Objects.newArrayList();
        List<StateBean> lc = TablesFactory.getTable(table).getStates();
        for(StateBean c:lc){
            l.add(c.toMap());
        }
        return l;
    }

    public boolean isAccessAllowed(String table){
        return TablesFactory.getTable(table).isAccessAllowed();
    }

    public boolean isLogAccessAllowed(String table, String id) throws NotFoundException, AccessDeniedException {
        return TablesFactory.getTable(table).isLogAccessAllowed(TablesFactory.getTable(table).get(id));
    }

    public boolean isImportAccessAllowed(String table){
        return TablesFactory.getTable(table).isImportAllowed();
    }
}
