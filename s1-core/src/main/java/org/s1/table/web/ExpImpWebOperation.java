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

package org.s1.table.web;

import org.s1.S1SystemError;
import org.s1.cluster.dds.beans.Id;
import org.s1.cluster.dds.file.FileStorage;
import org.s1.objects.Objects;
import org.s1.table.Table;
import org.s1.table.Tables;
import org.s1.table.format.Query;
import org.s1.weboperation.MapWebOperation;
import org.s1.weboperation.UploadWebOperation;
import org.s1.weboperation.WebOperationMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
        return Tables.get(t);
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
        }catch (Throwable e){
            LOG.warn("Cannot initialize format ("+cls+") for type "+type+": "+e.getClass().getName()+": "+e.getMessage());
            throw S1SystemError.wrap(e);
        }
        return f;
    }

    @WebOperationMethod
    public Map<String,Object> viewData(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        String id = Objects.get(params,"id");
        final String type = Objects.get(params,"type");
        String database = Objects.get(params,"database");
        String collection = Objects.get(params,"collection", UploadWebOperation.COLLECTION);

        ExpImpFormat.PreviewBean pb = null;
        FileStorage.FileReadBean b = null;
        try{
            b = FileStorage.read(new Id(database,collection,id));
            ExpImpFormat f = getFormat(type);
            pb = f.preview(b);
        }finally {
            FileStorage.closeAfterRead(b);
        }

        return Objects.newHashMap("schema",pb.getSchema().toMap(),"list",pb.getList(),"count",pb.getCount());
    }

    @WebOperationMethod
    public Map<String,Object> importData(final Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        String id = Objects.get(params,"id");
        final String type = Objects.get(params,"type");
        String database = Objects.get(params,"database");
        String collection = Objects.get(params,"collection", UploadWebOperation.COLLECTION);
        final List<Map<String,Object>> list = Objects.newArrayList();

        FileStorage.FileReadBean b = null;
        try{
            b = FileStorage.read(new Id(database,collection,id));
            ExpImpFormat f = getFormat(type);
            f.doImport(list, b, getTable(params));
        }finally {
            FileStorage.closeAfterRead(b);
        }

        return Objects.newHashMap("list",list);
    }

    @WebOperationMethod
    public Map<String,Object> exportData(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception{
        String id = UUID.randomUUID().toString();
        String database = Objects.get(params,"database");
        String collection = Objects.get(params,"collection", UploadWebOperation.COLLECTION);
        String type = Objects.get(params,"type");
        final ExpImpFormat format = getFormat(type);
        Map<String,Object> ctx = Objects.get(params,"context");
        Query q = getQuery(params);

        int skip = 0;
        int i=0;
        long c = 0;
        //prepare file
        format.prepareExport(getTable(params).getSchema(), params, request, response);
        while(true){
            List<Map<String,Object>> list = Objects.newArrayList();
            c = getTable(params).list(list, q, null, null, skip, 10, ctx);
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

        FileStorage.FileWriteBean b = null;
        try{
            b = FileStorage.createFileWriteBean(new Id(database,collection,id), meta);
            format.writeExport(b.getOutputStream());
            FileStorage.save(b);
        }finally {
            FileStorage.closeAfterWrite(b);
        }

        return Objects.newHashMap("id",id);
    }

    @Override
    protected Map<String, Object> process(String method, Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return processClassMethods(this,method,params,request,response);
    }
}
