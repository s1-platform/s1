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

package org.s1.cluster.dds.file;

import org.s1.S1SystemError;
import org.s1.cluster.dds.*;
import org.s1.table.NotFoundException;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.misc.IOUtils;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Distributed file storage - use it for storing files, internally uses {@link org.s1.cluster.dds.file.FileLocalStorage}
 */
public class FileStorage extends DistributedDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorage.class);

    @Override
    public void runWriteCommand(final CommandBean cmd) {
        if("write".equals(cmd.getCommand())){
            String nodeId = Objects.get(String.class,cmd.getParams(),"nodeId");
            if(!nodeId.equals(DDSCluster.getCurrentNodeId())){

                try{
                    FileExchange.getFile(
                            cmd.getCollection(),
                            cmd.getEntity(),
                            new Closure<FileExchange.GetFileBean, Object>() {
                                @Override
                                public Object call(final FileExchange.GetFileBean resp) throws ClosureException {
                                    getLocalStorage().write(
                                            cmd.getCollection(),
                                            cmd.getEntity(),
                                            new Closure<OutputStream, Boolean>() {
                                                @Override
                                                public Boolean call(OutputStream os) throws ClosureException {
                                                    try {
                                                        IOUtils.copy(resp.getInputStream(), os, 0, resp.getSize());
                                                    } catch (IOException e) {
                                                        throw S1SystemError.wrap(e);
                                                    }
                                                    return true;
                                                }
                                            },
                                            Objects.get(FileMetaBean.class, cmd.getParams(), "meta")
                                    );
                                    return null;
                                }
                            }
                    );
                }catch (ClosureException e){
                    throw e.toSystemError();
                }
            }
        }else if("remove".equals(cmd.getCommand())){
            getLocalStorage().remove(
                    cmd.getCollection(),
                    cmd.getEntity()
            );
        }
    }

    private static FileLocalStorage localStorage;
    
    private static synchronized FileLocalStorage getLocalStorage(){
        if(localStorage==null){
            String cls = Options.getStorage().getSystem("fileStorage.localStorageClass",FileLocalStorage.class.getName());
            try{
                localStorage = (FileLocalStorage)Class.forName(cls).newInstance();
            }catch (Exception e){
                LOG.warn("Cannot initialize FileLocalStorage ("+cls+"): "+e.getMessage(),e);
            }
        }
        return localStorage;
    }

    /**
     *
     * @param cls
     * @param group
     * @param id
     * @param cl
     * @param <T>
     * @return
     * @throws org.s1.table.NotFoundException
     * @throws org.s1.misc.ClosureException
     */
    public static <T> T read(Class<T> cls, String group, String id, Closure<FileReadBean,T> cl) throws NotFoundException, ClosureException{
        return Objects.cast(read(group,id,cl),cls);
    }

    /**
     *
     * @param group
     * @param id
     * @param cl
     * @param <T>
     * @return
     * @throws NotFoundException
     * @throws org.s1.misc.ClosureException
     */
    public static <T> T read(String group, String id, Closure<FileReadBean,T> cl) throws NotFoundException, ClosureException {
        return getLocalStorage().read(group,id,cl);
    }

    /**
     *
     * @param group
     * @param id
     * @param closure
     * @param meta
     * @throws org.s1.misc.ClosureException
     */
    public static void write(String group, String id, Closure<OutputStream,Boolean> closure, FileMetaBean meta) throws ClosureException{
        getLocalStorage().write(group, id, closure, meta);
        DDSCluster.call(new MessageBean(FileStorage.class, null, group, id, "write", Objects.newHashMap(String.class, Object.class,
                "nodeId", DDSCluster.getCurrentNodeId(),
                "meta", meta)));
    }

    /**
     *
     * @param group
     * @param id
     */
    public static void remove(String group, String id){
        DDSCluster.call(new MessageBean(FileStorage.class, null, group, id, "remove", null));
    }

    /**
     *
     */
    public static class FileMetaBean implements Serializable{

        private String name;
        private String ext;
        private String contentType;
        private long size;
        private Date lastModified;
        private Date created;
        private Map<String,Object> info;

        public FileMetaBean(){}

        public FileMetaBean(String name, String ext, String contentType, Map<String, Object> info) {
            this(name,ext,contentType,0,info);
        }

        public FileMetaBean(String name, String ext, String contentType, long size, Map<String, Object> info) {
            if(Objects.isNullOrEmpty(contentType))
                contentType = "application/octet-stream";
            if(info == null)
                info = Objects.newHashMap();
            if(size<0)
                size = 0;
            this.name = name;
            this.ext = ext;
            this.contentType = contentType;
            this.size = size;
            this.info = info;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setExt(String ext) {
            this.ext = ext;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public void setLastModified(Date lastModified) {
            this.lastModified = lastModified;
        }

        public void setCreated(Date created) {
            this.created = created;
        }

        public void setInfo(Map<String, Object> info) {
            this.info = info;
        }

        public String getName() {
            return name;
        }

        public String getExt() {
            return ext;
        }

        public String getContentType() {
            return contentType;
        }

        public long getSize() {
            return size;
        }

        public Date getLastModified() {
            return lastModified;
        }

        public Date getCreated() {
            return created;
        }

        public Map<String, Object> getInfo() {
            return Objects.copy(info);
        }

        public Map<String,Object> toMap(){
            Map<String,Object> m = Objects.copy(info);
            m.putAll(Objects.newHashMap(String.class,Object.class,
                    "name",name,
                    "ext",ext,
                    "size",size,
                    "contentType",contentType,
                    "lastModified",lastModified,
                    "created",created
            ));
            return m;
        }

        public void fromMap(Map<String,Object> m){
            m = Objects.copy(m);
            name = Objects.get(m,"name");
            ext = Objects.get(m,"ext");
            contentType = Objects.get(m,"contentType","application/octet-stream");
            size = Objects.get(Long.class,m,"size",0L);
            lastModified = Objects.get(m,"lastModified");
            created = Objects.get(m,"created");
            m.remove("name");
            m.remove("ext");
            m.remove("contentType");
            m.remove("size");
            m.remove("lastModified");
            m.remove("created");
            info = m;
        }
    }

    /**
     *
     */
    public static class FileReadBean{
        private InputStream inputStream;
        private FileMetaBean meta;

        public FileReadBean(InputStream inputStream, FileMetaBean meta) {
            this.inputStream = inputStream;
            this.meta = meta;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public FileMetaBean getMeta() {
            return meta;
        }
    }

}