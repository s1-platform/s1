package org.s1.cluster.datasource;

import org.s1.S1SystemError;
import org.s1.format.json.JSONFormat;
import org.s1.format.json.JSONFormatException;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.misc.FileUtils;
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
 * Date: 24.01.14
 * Time: 11:32
 */
public class FileLocalStorage {

    private static final Logger LOG = LoggerFactory.getLogger(FileLocalStorage.class);

    /**
     *
     * @param group
     * @param id
     * @param cl
     * @param <T>
     * @return
     * @throws NotFoundException
     * @throws ClosureException
     */
    public <T> T read(String group, String id, Closure<FileStorage.FileReadBean,T> cl) throws NotFoundException, ClosureException {
        String dir = getBaseDirectory(group);

        FileStorage.FileMetaBean meta = new FileStorage.FileMetaBean();
        String ms = null;
        try {
            ms = FileUtils.readFileToString(new File(dir + File.separator + id + ".json"), "UTF-8");
        } catch (IOException e) {
            throw S1SystemError.wrap(e);
        }
        if(ms==null)
            throw new NotFoundException("Local file not found group: '" + group+"', id: '"+id+"'");

        try{
            meta.fromMap(Objects.fromWire(JSONFormat.evalJSON(ms)));
        }catch(JSONFormatException e){
            LOG.warn("File meta format error (group: "+group+", id: "+id+"): "+e.getMessage(),e);
            throw S1SystemError.wrap(e);
        }

        File f = new File(dir+File.separator+id);
        if (!f.exists()){
            throw new NotFoundException("Local file not found group: '" + group+"', id: '"+id+"'");
        }
        meta.setSize(f.length());
        FileInputStream fis = null;
        try{
            fis = new FileInputStream(f);
            if(LOG.isDebugEnabled())
                LOG.debug("File read successfully (group: "+group+", id: "+id+"): "+meta.toMap());
            return cl.call(new FileStorage.FileReadBean(fis, meta));
        }catch (IOException e){
            LOG.warn("File read error (group: "+group+", id: "+id+"): "+e.getMessage(),e);
            throw S1SystemError.wrap(e);
        }finally{
            IOUtils.closeQuietly(fis);
        }
    }

    /**
     *
     * @param group
     * @param id
     * @param closure
     * @param meta
     * @throws ClosureException
     */
    public void write(String group, String id, Closure<OutputStream,Boolean> closure, FileStorage.FileMetaBean meta) throws ClosureException {
        String dir = getBaseDirectory(group);
        if(!new File(dir+File.separator+id).exists())
            meta.setCreated(new Date());
        meta.setLastModified(new Date());
        Map<String,Object> m = meta.toMap();
        FileOutputStream fos = null;
        try{
            fos = new FileOutputStream(dir+File.separator+id);

            if(new Boolean(false).equals(closure.call(fos)))
                return;

            FileUtils.writeStringToFile(new File(dir+File.separator+id+".json"),JSONFormat.toJSON(Objects.toWire(m)),"UTF-8");
        }catch (IOException e){
            LOG.warn("File write error (group: "+group+", id: "+id+", meta: "+m+"): "+e.getMessage(),e);
            throw S1SystemError.wrap(e);
        }finally{
            IOUtils.closeQuietly(fos);
        }
        if(LOG.isDebugEnabled())
            LOG.debug("File write successfully (group: "+group+", id: "+id+", meta: "+m+")");
    }

    /**
     *
     * @param group
     * @param id
     */
    public void remove(String group, String id){
        String dir = getBaseDirectory(group);
        boolean i1 = new File(dir+File.separator+id).delete();
        boolean i2 = new File(dir+File.separator+id+".json").delete();
        if(LOG.isDebugEnabled())
            LOG.debug("File removed successfully("+(i1&&i2)+") group: "+group+", id: "+id);
    }

    private static String baseDirectory;

    /**
     *
     * @param path
     * @return
     */
    private static synchronized String getBaseDirectory(String path) {
        if(Objects.isNullOrEmpty(baseDirectory)){
            baseDirectory = Options.getStorage().getSystem("fileStorage.home", System.getProperty("user.home") + File.separator + ".s1-files");
        }
        File dir = new File(baseDirectory+File.separator+path);
        if(!dir.exists())
            dir.mkdirs();
        if(!dir.isDirectory())
            throw new S1SystemError("Directory error: "+baseDirectory+File.separator+path);
        return dir.getAbsolutePath();
    }

}
