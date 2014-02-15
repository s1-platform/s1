package org.s1.mongodb.cluster;

import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import org.s1.S1SystemError;
import org.s1.cluster.datasource.FileLocalStorage;
import org.s1.cluster.datasource.FileStorage;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.mongodb.MongoDBFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

/**
 * GridFS local file storage implementation
 */
public class GridFSFileStorage extends FileLocalStorage {

    private static final Logger LOG = LoggerFactory.getLogger(GridFSFileStorage.class);

    public static final String DB_INSTANCE = "gridFS";

    @Override
    public <T> T read(String group, String id, Closure<FileStorage.FileReadBean, T> cl) throws NotFoundException, ClosureException {
        GridFS fs = new GridFS(MongoDBConnectionHelper.getConnection(DB_INSTANCE), group);
        GridFSDBFile o = fs.findOne(id);
        if (o == null)
            throw new NotFoundException("GridFS file not found group: '" + group+"', id: '"+id+"'");
        FileStorage.FileMetaBean fb = new FileStorage.FileMetaBean();
        fb.fromMap(MongoDBFormat.toMap(o.getMetaData()));
        fb.setSize(o.getLength());
        fb.setContentType(o.getContentType());
        if(LOG.isDebugEnabled())
            LOG.debug("Read file group:"+group+", id:"+id+", meta:"+fb.toMap());
        return cl.call(new FileStorage.FileReadBean(o.getInputStream(), fb));
    }

    @Override
    public void write(String group, String id, Closure<OutputStream, Boolean> closure, FileStorage.FileMetaBean meta) throws ClosureException {
        meta.setLastModified(new Date());
        meta.setCreated(new Date());
        GridFS fs = new GridFS(MongoDBConnectionHelper.getConnection(DB_INSTANCE), group);
        fs.remove(id);

        GridFSInputFile gfsFile = fs.createFile(id);
        gfsFile.setContentType(meta.getContentType());
        gfsFile.setMetaData(MongoDBFormat.fromMap(meta.toMap()));
        if(Boolean.FALSE.equals(closure.call(gfsFile.getOutputStream())))
            return;
        try {
            gfsFile.getOutputStream().close();
        } catch (IOException e) {
            throw S1SystemError.wrap(e);
        }
        if(LOG.isDebugEnabled())
            LOG.debug("File added successfully group:"+group+", id:"+id+", meta:"+meta.toMap());
    }

    @Override
    public void remove(String group, String id) {
        GridFS fs = new GridFS(MongoDBConnectionHelper.getConnection(DB_INSTANCE), group);
        fs.remove(id);
        if(LOG.isDebugEnabled())
            LOG.debug("File removed successfully group:"+group+", id:"+id);
    }
}
