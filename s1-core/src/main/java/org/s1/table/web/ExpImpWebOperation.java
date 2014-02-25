package org.s1.table.web;

import org.s1.cluster.datasource.FileStorage;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;
import org.s1.objects.schema.ObjectSchema;
import org.s1.table.Table;
import org.s1.table.TablesFactory;
import org.s1.table.format.FieldsMask;
import org.s1.table.format.Query;
import org.s1.table.format.Sort;
import org.s1.weboperation.MapWebOperation;
import org.s1.weboperation.WebOperationMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Table export/import web operation
 */
public abstract class ExpImpWebOperation extends MapWebOperation{

    protected Table getTable(Map<String,Object> params){
        String t = Objects.get(params,"table");
        return TablesFactory.getTable(t);
    }

    protected Query getQuery(Map<String,Object> params){
        Query q = new Query();
        Map<String,Object> mq = Objects.get(params,"search");
        if(!Objects.isNullOrEmpty(mq)){
            q.fromMap(mq);
        }
        return q;
    }

    protected abstract List<Map<String,Object>> getSampleDataFromImport();

    protected abstract ObjectSchema getSchemaFromImport();

    protected abstract void prepareExport(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response);

    protected abstract void addPortionToExport(int i, List<Map<String,Object>> list);

    protected abstract void finishExport(int files, long count);

    @WebOperationMethod
    public Map<String,Object> viewData(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        String id = Objects.get(params,"id");
        String group = Objects.get(params,"group");
        FileStorage.read(group,id,new Closure<FileStorage.FileReadBean, Object>() {
            @Override
            public Object call(FileStorage.FileReadBean input) throws ClosureException {
                return null;
            }
        });
        return Objects.newHashMap("schema",getSampleDataFromImport(),"list",getSchemaFromImport());
    }

    @WebOperationMethod
    public Map<String,Object> exportData(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        List<Map<String,Object>> list = Objects.newArrayList();
        long c = 0;

        String text = Objects.get(params,"text");
        Map<String,Object> ctx = Objects.get(params,"context");
        Query q = getQuery(params);
        int skip = 0;
        int i=0;
        //prepare file
        prepareExport(params, request, response);
        while(true){
            c = getTable(params).list(list, text, getQuery(params), null, null, skip, 10, ctx);
            skip+=10;
            if(list.size()==0)
                break;
            addPortionToExport(i, list);
            i++;
        } 
        //write file
        finishExport(i, c);

        return null;
    }

    @WebOperationMethod
    public Map<String,Object> importData(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        List<Map<String,Object>> list = Objects.newArrayList();

        return Objects.newHashMap("list",getTable(params).doImport(list));
    }

    @Override
    protected Map<String, Object> process(String method, Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return processClassMethods(this,method,params,request,response);
    }
}
