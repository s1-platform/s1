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

package org.s1.weboperation;

import com.hazelcast.core.IMap;
import org.s1.S1SystemError;
import org.s1.cluster.HazelcastWrapper;
import org.s1.cluster.dds.beans.Id;
import org.s1.cluster.dds.file.FileStorage;
import org.s1.misc.IOUtils;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.s1.table.errors.NotFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Operation for file uploading and downloading to/from FileStorage
 */
public class UploadWebOperation extends MapWebOperation {

    private static final IMap<String,Long> progress = HazelcastWrapper.getInstance().getMap("upload.progress");

    /**
     * Default group
     */
    public static final String COLLECTION = "upload";

    @Override
    protected Map<String, Object> process(String method, Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return processClassMethods(this,method,params,request,response);
    }

    protected void writeFile(Id id, FileParameter fp){
        String taskId = id.getEntity();
        progress.put(taskId,0L);
        FileStorage.FileWriteBean b = null;
        try{
            b = FileStorage.createFileWriteBean(id, new FileStorage.FileMetaBean(fp.getName(), fp.getExt(), fp.getContentType(), fp.getSize(), null));
            try {
                long l = 1024L;
                for(long i=0;i<fp.getSize();i+=l) {
                    IOUtils.copy(fp.getInputStream(), b.getOutputStream(), 0, l);
                    progress.put(taskId, i);
                }
            } catch (IOException e) {
                throw S1SystemError.wrap(e);
            }
            FileStorage.save(b);
        }finally {
            FileStorage.closeAfterWrite(b);
            progress.remove(taskId);
        }
    }

    @WebOperationMethod
    public Map<String,Object> getProgress(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> result = Objects.newSOHashMap();
        String taskId = Objects.get(params,"id");
        Objects.assertNotEmpty("id",taskId);
        long p = Objects.get(Long.class,(Map)progress,taskId);
        if(!progress.containsKey(taskId))
            p = -1;
        return asMap(p);
    }

    @WebOperationMethod
    public Map<String,Object> startProgress(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> result = Objects.newSOHashMap();
        String id = Objects.get(params,"id",UUID.randomUUID().toString());
        progress.put(id,0L);
        return asMap(id);
    }

    /**
     * Upload file
     *
     * @param params {group:"...default is GROUP...", file: FileParameter}
     * @param result will be {id:"..."}
     * @throws Exception
     * @see org.s1.weboperation.MapWebOperation.FileParameter
     */
    @WebOperationMethod
    public Map<String,Object> upload(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String,Object> result = Objects.newSOHashMap();
        String database = (String) params.get("database");
        String collection = (String) params.get("collection");
        if (Objects.isNullOrEmpty(collection))
            collection = COLLECTION;

        if (params.containsKey("file")) {
            String id = Objects.get(params,"id",UUID.randomUUID().toString());

            final FileParameter fp = (FileParameter) params.get("file");

            writeFile(new Id(database,collection,id),fp);
            result.put("id", id);
        } else {
            List<String> ids = new ArrayList<String>();
            int i = 0;
            while (result.containsKey("file" + i)) {
                String id = UUID.randomUUID().toString();

                final FileParameter fp = (FileParameter) params.get("file" + i);

                writeFile(new Id(database,collection,id),fp);

                ids.add(id);
            }
            result.put("ids", ids);
        }
        return result;
    }

    /**
     * Download file
     *
     * @param params   {group:"...default is GROUP...", id:"..."}
     * @param response
     */
    @WebOperationMethod
    public Map<String,Object> download(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String database = Objects.get(params, "database");
        String collection = Objects.get(params, "collection", COLLECTION);
        String id = Objects.get(params, "id");

        FileStorage.FileReadBean b = null;
        try{
            b = FileStorage.read(new Id(database,collection,id));
            response.setContentType(b.getMeta().getContentType());
            try {
                IOUtils.copy(b.getInputStream(), response.getOutputStream());
            } catch (IOException e) {
                throw S1SystemError.wrap(e);
            }
        }catch (NotFoundException e) {
            response.setStatus(404);
        }finally {
            FileStorage.closeAfterRead(b);
        }
        return null;
    }

    /**
     * Download file from storage with content-disposition
     *
     * @param params   {group:"...default is GROUP...", id:"...", name:"...default will be taken from FileMetaBean.name ..."}
     * @param response
     */
    @WebOperationMethod
    public Map<String,Object> downloadAsFile(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String database = Objects.get(params, "database");
        String collection = Objects.get(params, "collection", COLLECTION);
        String id = Objects.get(params, "id");
        String name = Objects.get(params, "name");

        FileStorage.FileReadBean b = null;
        try{
            b = FileStorage.read(new Id(database,collection,id));
            response.setContentType(b.getMeta().getContentType());
            if (Objects.isNullOrEmpty(name)) {
                name = b.getMeta().getName();
                if (name.length() > 100)
                    name = name.substring(0, 100);
                if (!Objects.isNullOrEmpty(b.getMeta().getExt()))
                    name += "." + b.getMeta().getExt();
            }
            response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(name, "UTF-8"));
            try {
                IOUtils.copy(b.getInputStream(), response.getOutputStream());
            } catch (IOException e) {
                throw S1SystemError.wrap(e);
            }
        }catch (NotFoundException e) {
            response.setStatus(404);
        }finally {
            FileStorage.closeAfterRead(b);
        }
        return null;
    }


}
