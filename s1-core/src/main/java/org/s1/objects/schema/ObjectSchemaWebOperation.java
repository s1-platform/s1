package org.s1.objects.schema;

import org.s1.objects.ObjectDiff;
import org.s1.objects.Objects;
import org.s1.weboperation.MapWebOperation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Web operation for object schema validating
 */
public class ObjectSchemaWebOperation extends MapWebOperation {

    @Override
    protected Map<String, Object> process(String method, Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String,Object> res = Objects.newHashMap();
        if("validate".equals(method)){
            Map<String,Object> s = Objects.get(Map.class,params,"schema");
            Map<String,Object> d = Objects.get(Map.class,params,"data",Objects.newHashMap());
            Map<String,Object> c = Objects.get(Map.class, params, "ctx");
            ObjectSchema schema = new ObjectSchema();
            schema.fromMap(s);
            ObjectSchema.ValidateResultBean vr = schema.validateQuite(d, true, true, c);
            res.put("schema",vr.getResolvedSchema().toMap());
            res.put("data",vr.getValidatedData());
            if(Objects.get(Boolean.class,params,"dataDiff",false)){
                List<Map<String,Object>> list = Objects.newArrayList();
                List<ObjectDiff.DiffBean> l = Objects.diff(d,vr.getValidatedData());
                for(ObjectDiff.DiffBean b:l){
                    list.add(Objects.newHashMap(String.class,Object.class,
                            "path",b.getPath(),"old",b.getOldValue(),"new",b.getNewValue()
                            ));
                }
                res.put("dataDiff",list);
            }
            if(Objects.get(Boolean.class,params,"schemaDiff",false)){
                List<Map<String,Object>> list = Objects.newArrayList();
                List<ObjectDiff.DiffBean> l = Objects.diff(s,vr.getResolvedSchema().toMap());
                for(ObjectDiff.DiffBean b:l){
                    list.add(Objects.newHashMap(String.class,Object.class,
                            "path",b.getPath(),"old",b.getOldValue(),"new",b.getNewValue()
                    ));
                }
                res.put("schemaDiff",list);
            }
        }else{
            throwMethodNotFound(method);
        }
        return res;
    }
}
