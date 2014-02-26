package org.s1.table.web;

import org.s1.S1SystemError;
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
import org.s1.user.AccessDeniedException;
import org.s1.weboperation.MapWebOperation;
import org.s1.weboperation.UploadWebOperation;
import org.s1.weboperation.WebOperationMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Table export/import web operation
 */
public class ExpImpWebOperation extends MapWebOperation{

    private static final Logger LOG = LoggerFactory.getLogger(ExpImpWebOperation.class);

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

    protected ExpImpFormat getFormat(String type){
        ExpImpFormat f = null;
        String cls = null;
        List<Map<String,Object>> l = Objects.get(config,"formats");
        if(l!=null){
            for(Map<String,Object> m:l){
                if(type.equals(Objects.get(m,"name"))){
                    cls = Objects.get(m,"class");
                    break;
                }
            }
        }
        try{
            f = (ExpImpFormat) Class.forName(cls).newInstance();
        }catch (Exception e){
            LOG.warn("Cannot initialize format ("+cls+") for type "+type+": "+e.getClass().getName()+": "+e.getMessage());
            throw S1SystemError.wrap(e);
        }
        return f;
    }

    @WebOperationMethod
    public Map<String,Object> viewData(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        String id = Objects.get(params,"id");
        final String type = Objects.get(params,"type");
        String group = Objects.get(params,"group", UploadWebOperation.GROUP);
        ExpImpFormat.PreviewBean pb = FileStorage.read(group,id,new Closure<FileStorage.FileReadBean, ExpImpFormat.PreviewBean>() {
            @Override
            public ExpImpFormat.PreviewBean call(FileStorage.FileReadBean input) throws ClosureException {
                ExpImpFormat f = getFormat(type);
                return f.preview(input);
            }
        });
        return Objects.newHashMap("schema",pb.getSchema().toMap(),"list",pb.getList(),"count",pb.getCount());
    }

    @WebOperationMethod
    public Map<String,Object> importData(final Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        String id = Objects.get(params,"id");
        final String type = Objects.get(params,"type");
        String group = Objects.get(params,"group", UploadWebOperation.GROUP);
        final List<Map<String,Object>> list = Objects.newArrayList();

        try {
            FileStorage.read(group,id,new Closure<FileStorage.FileReadBean, Object>() {
                @Override
                public Object call(FileStorage.FileReadBean input) throws ClosureException {
                    ExpImpFormat f = getFormat(type);
                    try {
                        f.doImport(list, input, getTable(params));
                    } catch (AccessDeniedException e) {
                        throw ClosureException.wrap(e);
                    }
                    return null;
                }
            });
        } catch (ClosureException e) {
            if(e.getCause()!=null)
                throw (Exception)e.getCause();
            throw e;
        }

        return Objects.newHashMap("list",list);
    }

    @WebOperationMethod
    public Map<String,Object> exportData(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        String id = UUID.randomUUID().toString();
        String group = Objects.get(params,"group", UploadWebOperation.GROUP);
        String type = Objects.get(params,"type");
        final ExpImpFormat format = getFormat(type);
        String text = Objects.get(params,"text");
        Map<String,Object> ctx = Objects.get(params,"context");
        Query q = getQuery(params);

        int skip = 0;
        int i=0;
        long c = 0;
        //prepare file
        format.prepareExport(getTable(params).getSchema(), params, request, response);
        while(true){
            List<Map<String,Object>> list = Objects.newArrayList();
            c = getTable(params).list(list, text, q, null, null, skip, 10, ctx);
            skip+=10;
            if(list.size()==0)
                break;
            format.addPortionToExport(i, list);
            i++;
        } 
        //write file
        format.finishExport(i, c);

        FileStorage.FileMetaBean meta = new FileStorage.FileMetaBean("export",null,null,null);
        format.setFileMeta(meta);
        FileStorage.write(group,id,new Closure<OutputStream, Boolean>() {
            @Override
            public Boolean call(OutputStream input) throws ClosureException {
                format.writeExport(input);
                return null;
            }
        },meta);

        return Objects.newHashMap("id",id,"group",group);
    }

    @Override
    protected Map<String, Object> process(String method, Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return processClassMethods(this,method,params,request,response);
    }
}
