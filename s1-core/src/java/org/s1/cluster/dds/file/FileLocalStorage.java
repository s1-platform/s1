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
import org.s1.cluster.dds.beans.Id;
import org.s1.format.json.JSONFormat;
import org.s1.format.json.JSONFormatException;
import org.s1.misc.FileUtils;
import org.s1.misc.IOUtils;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.s1.table.errors.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * Local file storage
 */
public class FileLocalStorage {

    private static final Logger LOG = LoggerFactory.getLogger(FileLocalStorage.class);

    /**
     *
     * @param id
     * @return
     * @throws NotFoundException
     */
    public FileStorage.FileReadBean read(Id id) throws NotFoundException {
        String dir = getBaseDirectory(id.getDatabase(),id.getCollection());

        FileStorage.FileMetaBean meta = new FileStorage.FileMetaBean();
        String ms = null;
        try {
            ms = FileUtils.readFileToString(new File(dir + File.separator + id.getEntity() + ".json"), "UTF-8");
        } catch (IOException e) {
            throw S1SystemError.wrap(e);
        }
        if(ms==null)
            throw new NotFoundException("Local file not found : " + id);

        try{
            meta.fromMap(Objects.fromWire(JSONFormat.evalJSON(ms)));
        }catch(JSONFormatException e){
            LOG.warn("File meta format error ("+id+"): "+e.getMessage(),e);
            throw S1SystemError.wrap(e);
        }

        File f = new File(dir+File.separator+id.getEntity());
        if (!f.exists()){
            throw new NotFoundException("Local file not found: " + id);
        }
        meta.setSize(f.length());
        FileInputStream fis = null;
        try{
            fis = new FileInputStream(f);
            if(LOG.isDebugEnabled())
                LOG.debug("File read successfully ("+id+"): "+meta.toMap());
            return new FileStorage.FileReadBean(fis, meta);
        }catch (IOException e){
            LOG.warn("File read error ("+id+"): "+e.getMessage(),e);
            throw S1SystemError.wrap(e);
        }
    }

    /**
     *
     * @param b
     */
    public void closeAfterRead(FileStorage.FileReadBean b){
        if(b!=null)
            IOUtils.closeQuietly(b.getInputStream());
    }

    /**
     *
     * @param id
     * @param meta
     * @return
     */
    public FileStorage.FileWriteBean createFileWriteBean(Id id, FileStorage.FileMetaBean meta) {
        String dir = getBaseDirectory(id.getDatabase(),id.getCollection());
        if(!new File(dir+File.separator+id).exists())
            meta.setCreated(new Date());
        meta.setLastModified(new Date());
        Map<String,Object> m = meta.toMap();
        FileOutputStream fos = null;
        try{
            fos = new FileOutputStream(dir+File.separator+id.getEntity());

            return new FileStorage.FileWriteBean(id,fos,meta);

        }catch (IOException e){
            LOG.warn("File write error (id:"+id+", meta: "+m+"): "+e.getMessage(),e);
            throw S1SystemError.wrap(e);
        }
    }

    /**
     *
     * @param b
     */
    public void save(FileStorage.FileWriteBean b){
        try {
            String dir = getBaseDirectory(b.getId().getDatabase(),b.getId().getCollection());
            FileUtils.writeStringToFile(new File(dir + File.separator + b.getId().getEntity() + ".json"), JSONFormat.toJSON(Objects.toWire(b.getMeta().toMap())), "UTF-8");

        }catch (IOException e){
            LOG.warn("File write error (id: "+b.getId()+", meta: "+b.getMeta()+"): "+e.getMessage(),e);
            throw S1SystemError.wrap(e);
        }
        if(LOG.isDebugEnabled())
            LOG.debug("File write successfully (id: "+b.getId()+", meta: "+b.getMeta()+")");
    }

    /**
     *
     * @param b
     */
    public void closeAfterWrite(FileStorage.FileWriteBean b){
        if(b!=null)
            IOUtils.closeQuietly(b.getOutputStream());
    }

    /**
     * Remove file
     *
     * @param id
     */
    public void remove(Id id){
        String dir = getBaseDirectory(id.getDatabase(),id.getCollection());
        boolean i1 = new File(dir+File.separator+id.getEntity()).delete();
        boolean i2 = new File(dir+File.separator+id.getEntity()+".json").delete();
        if(LOG.isDebugEnabled())
            LOG.debug("File removed successfully("+(i1&&i2)+") id: "+id);
    }

    private static String baseDirectory;

    /**
     *
     * @param database
     * @param collection
     * @return
     */
    private static synchronized String getBaseDirectory(String database, String collection) {
        if(Objects.isNullOrEmpty(database))
            database = "default";
        if(Objects.isNullOrEmpty(collection))
            collection = "default";
        if(Objects.isNullOrEmpty(baseDirectory)){
            baseDirectory = Options.getStorage().getSystem("fileStorage.home", System.getProperty("user.home") + File.separator + ".s1-files");
        }
        File dir = new File(baseDirectory+File.separator+database+File.separator+collection);
        if(!dir.exists())
            dir.mkdirs();
        if(!dir.isDirectory())
            throw new S1SystemError("Directory error: "+baseDirectory+File.separator+database+File.separator+collection);
        return dir.getAbsolutePath();
    }

}
