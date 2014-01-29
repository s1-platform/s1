package org.s1.cluster.datasource;

import org.s1.S1SystemError;
import org.s1.cluster.node.ClusterNode;
import org.s1.cluster.node.NodeFileExchange;
import org.s1.format.json.JSONFormat;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.misc.IOUtils;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 20.01.14
 * Time: 14:33
 */
public class FileStorage extends DistributedDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorage.class);

    @Override
    public void runWriteCommand(String command, final Map<String, Object> params) {
        if("write".equals(command)){
            String nodeId = Objects.get(String.class,params,"nodeId");
            if(!nodeId.equals(ClusterNode.getCurrentNodeId())){

                try{
                    NodeFileExchange.getFile(
                            Objects.get(String.class,params,"group"),
                            Objects.get(String.class,params,"id"),
                            new Closure<NodeFileExchange.GetFileBean, Object>(){
                                @Override
                                public Object call(final NodeFileExchange.GetFileBean resp) throws ClosureException {
                                    getLocalStorage().write(
                                            Objects.get(String.class, params, "group"),
                                            Objects.get(String.class, params, "id"),
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
                                            Objects.get(FileMetaBean.class, params, "meta")
                                    );
                                    return null;
                                }
                            }
                    );
                }catch (ClosureException e){
                    throw e.toSystemError();
                }
                /*
                        [type:"FileStorage",id:params.id,group:params.group],{copy->
                        FileStorage.getInstance().realWriteFile(params.group,params.id,{fos->
                                copy(fos);
                    },params.fileName,params.ext,params.contentType,params.meta);
                })*/


            }
        }else if("remove".equals(command)){
            getLocalStorage().remove(
                    Objects.get(String.class, params, "group"),
                    Objects.get(String.class, params, "id")
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

    public static <T> T read(Class<T> cls, String group, String id, Closure<FileReadBean,T> cl) throws NotFoundException, ClosureException{
        return Objects.cast(read(group,id,cl),cls);
    }

    public static <T> T read(String group, String id, Closure<FileReadBean,T> cl) throws NotFoundException, ClosureException {
        return getLocalStorage().read(group,id,cl);
    }

    public static void write(String group, String id, Closure<OutputStream,Boolean> closure, FileMetaBean meta) throws ClosureException{
        getLocalStorage().write(group, id, closure, meta);
        ClusterNode.call(FileStorage.class,"write",Objects.newHashMap(String.class,Object.class,
                "nodeId",ClusterNode.getCurrentNodeId(),
                "group",group,
                "id",id,
                "meta",meta),group+":"+id);
    }

    public static void remove(String group, String id){
        ClusterNode.call(FileStorage.class, "remove", Objects.newHashMap(String.class,Object.class,
                "group",group,
                "id",id
        ),group+":"+id);
    }

    public static class FileMetaBean implements Serializable{

        private String name;
        private String ext;
        private String contentType;
        private long size;
        private Date lastModified;
        private Date created;
        private Map<String,Object> info;

        public FileMetaBean(){}

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
