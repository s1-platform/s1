package org.s1.table.web;

import org.s1.objects.Objects;
import org.s1.table.*;
import org.s1.table.format.FieldsMask;
import org.s1.table.format.Query;
import org.s1.table.format.Sort;
import org.s1.weboperation.MapWebOperation;
import org.s1.weboperation.WebOperation;
import org.s1.weboperation.WebOperationMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

/**
 * Table web operation
 */
public class TableWebOperation extends MapWebOperation{

    protected Table getTable(Map<String,Object> params){
        String t = Objects.get(params,"table");
        return TablesFactory.getTable(t);
    }

    @WebOperationMethod
    public Map<String,Object> get(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        String id = Objects.get(params,"id");
        Map<String,Object> ctx = Objects.get(params,"context");
        return getTable(params).get(id,ctx);
    }

    protected Query getQuery(Map<String,Object> params){
        Query q = new Query();
        Map<String,Object> mq = Objects.get(params,"search");
        if(!Objects.isNullOrEmpty(mq)){
            q.fromMap(mq);
        }
        return q;
    }

    protected Sort getSort(Map<String,Object> params){
        Sort q = new Sort();
        Map<String,Object> mq = Objects.get(params,"sort");
        if(!Objects.isNullOrEmpty(mq)){
            q.fromMap(mq);
        }
        return q;
    }

    protected FieldsMask getFieldsMask(Map<String,Object> params){
        FieldsMask q = new FieldsMask();
        Map<String,Object> mq = Objects.get(params,"fields");
        if(!Objects.isNullOrEmpty(mq)){
            q.fromMap(mq);
        }
        return q;
    }

    @WebOperationMethod
    public Map<String,Object> list(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        String text = Objects.get(params,"text");
        Map<String,Object> ctx = Objects.get(params,"context");
        int skip = Objects.get(Integer.class,params,"skip");
        int max = Objects.get(Integer.class,params,"max");

        List<Map<String,Object>> l = Objects.newArrayList();
        long c = getTable(params).list(l, text, getQuery(params), getSort(params), getFieldsMask(params), skip, max, ctx);
        return Objects.newHashMap("count",c,"list",l);
    }

    @WebOperationMethod
    public Map<String,Object> listLog(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        String id = Objects.get(params,"id");
        int skip = Objects.get(Integer.class,params,"skip");
        int max = Objects.get(Integer.class,params,"max");

        List<Map<String,Object>> l = Objects.newArrayList();
        long c = getTable(params).listLog(l, id, getQuery(params), skip, max);
        return Objects.newHashMap("count", c, "list", l);
    }

    @WebOperationMethod
    public Map<String,Object> countGroup(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        String field = Objects.get(params,"field");
        List<CountGroupBean> l = getTable(params).countGroup(field,getQuery(params));
        List<Map<String,Object>> r = Objects.newArrayList();
        for(CountGroupBean c:l){
            r.add(c.toMap());
        }
        return Objects.newHashMap("list", r);
    }

    @WebOperationMethod
    public Map<String,Object> aggregate(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        String field = Objects.get(params,"field");
        AggregationBean a = getTable(params).aggregate(field,getQuery(params));
        return a.toMap();
    }

    @WebOperationMethod
    public Map<String,Object> changeState(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        String id = Objects.get(params,"id");
        String action = Objects.get(params,"action");
        Map<String,Object> data = Objects.get(params, "data");
        Map<String,Object> foundation = Objects.get(params, "foundation");
        return getTable(params).changeState(id,action,data,foundation);
    }

    @WebOperationMethod
    public Map<String,Object> getAvailableActions(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        String id = Objects.get(params, "id");
        List<ActionBean> l = getTable(params).getAvailableActions(id);
        List<Map<String,Object>> r = Objects.newArrayList();
        for(ActionBean c:l){
            r.add(c.toMap());
        }
        return Objects.newHashMap("list", r);
    }

    @WebOperationMethod
    public Map<String,Object> getStates(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        List<StateBean> l = getTable(params).getStates();
        List<Map<String,Object>> r = Objects.newArrayList();
        for(StateBean c:l){
            r.add(c.toMap());
        }
        return Objects.newHashMap("list", r);
    }

    @WebOperationMethod
    public Map<String,Object> isAccessAllowed(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        return Objects.newHashMap("allowed",getTable(params).isAccessAllowed());
    }

    @WebOperationMethod
    public Map<String,Object> isImportAccessAllowed(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        return Objects.newHashMap("allowed",getTable(params).isImportAllowed());
    } 
    @WebOperationMethod
    public Map<String,Object> isLogAccessAllowed(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        String id = Objects.get(params,"id");
        return Objects.newHashMap("allowed",getTable(params).isLogAccessAllowed(getTable(params).get(id)));
    }

    @WebOperationMethod
    public Map<String,Object> getSchema(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        return getTable(params).getSchema().toMap();
    }

    @Override
    protected Map<String, Object> process(String method, Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return processClassMethods(this,method,params,request,response);
    }
}
