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

package org.s1.mongodb.cluster;

import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import org.s1.S1SystemError;
import org.s1.cluster.dds.beans.Id;
import org.s1.cluster.dds.file.FileLocalStorage;
import org.s1.cluster.dds.file.FileStorage;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.mongodb.MongoDBFormat;
import org.s1.table.errors.NotFoundException;
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

    @Override
    public FileStorage.FileReadBean read(Id id) throws NotFoundException {
        GridFS fs = new GridFS(MongoDBConnectionHelper.getConnection(id.getDatabase()), id.getCollection());
        GridFSDBFile o = fs.findOne(id.getEntity());
        if (o == null)
            throw new NotFoundException("GridFS file not found: " + id);
        FileStorage.FileMetaBean fb = new FileStorage.FileMetaBean();
        fb.fromMap(MongoDBFormat.toMap(o.getMetaData()));
        fb.setSize(o.getLength());
        fb.setContentType(o.getContentType());
        if(LOG.isDebugEnabled())
            LOG.debug("Read file: "+id+", meta:"+fb.toMap());
        return new FileStorage.FileReadBean(o.getInputStream(), fb);
    }

    @Override
    public void closeAfterRead(FileStorage.FileReadBean b) {

    }

    @Override
    public FileStorage.FileWriteBean createFileWriteBean(Id id, FileStorage.FileMetaBean meta) {
        meta.setLastModified(new Date());
        meta.setCreated(new Date());
        GridFS fs = new GridFS(MongoDBConnectionHelper.getConnection(id.getDatabase()), id.getCollection());
        fs.remove(id.getEntity());

        GridFSInputFile gfsFile = fs.createFile(id.getEntity());
        gfsFile.setContentType(meta.getContentType());
        gfsFile.setMetaData(MongoDBFormat.fromMap(meta.toMap()));

        GridFSFileWriteBean gridFSFileWriteBean = new GridFSFileWriteBean(id,gfsFile.getOutputStream(),meta);
        gridFSFileWriteBean.gfsFile = gfsFile;
        return gridFSFileWriteBean;
    }

    @Override
    public void save(FileStorage.FileWriteBean b) {
        try {
            ((GridFSFileWriteBean)b).gfsFile.getOutputStream().close();
        } catch (IOException e) {
            throw S1SystemError.wrap(e);
        }
        if(LOG.isDebugEnabled())
            LOG.debug("File added successfully:" + b.getId() + ", meta:" + b.getMeta().toMap());
    }

    private class GridFSFileWriteBean extends FileStorage.FileWriteBean{
        private GridFSInputFile gfsFile;
        public GridFSFileWriteBean(Id id, OutputStream outputStream, FileStorage.FileMetaBean meta) {
            super(id, outputStream, meta);
        }
    }

    @Override
    public void closeAfterWrite(FileStorage.FileWriteBean b) {

    }

    @Override
    public void remove(Id id) {
        GridFS fs = new GridFS(MongoDBConnectionHelper.getConnection(id.getDatabase()), id.getCollection());
        fs.remove(id.getEntity());
        if(LOG.isDebugEnabled())
            LOG.debug("File removed successfully:"+id);
    }
}
