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
import org.s1.cluster.dds.file.FileStorage;
import org.s1.format.json.JSONFormat;
import org.s1.format.json.JSONFormatException;
import org.s1.misc.IOUtils;
import org.s1.objects.Objects;
import org.s1.table.ImportResultBean;
import org.s1.table.Table;
import org.s1.user.AccessDeniedException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Default format
 */
public class ZipExpImpFormat extends ExpImpFormat{

    @Override
    protected PreviewBean preview(FileStorage.FileReadBean file) {
        try{
            List<Map<String,Object>> l = null;
            long count = 0;
            ZipInputStream zis = new ZipInputStream(file.getInputStream());
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if(entry.getName().equals("list_0.json") && !entry.isDirectory()){
                    l = Objects.get(Objects.fromWire(JSONFormat.evalJSON(IOUtils.toString(zis,"UTF-8"))),"list");
                }else if(entry.getName().equals("info.json") && !entry.isDirectory()){
                    count = Objects.get(Objects.fromWire(JSONFormat.evalJSON(IOUtils.toString(zis,"UTF-8"))),"count");
                }
            }
            return new PreviewBean(count,l);
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

    @Override
    protected void doImport(List<ImportResultBean> list, FileStorage.FileReadBean file, Table table)
            throws AccessDeniedException {
        try{
            ZipInputStream zis = new ZipInputStream(file.getInputStream());
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if(entry.getName().startsWith("list_") && !entry.isDirectory()){
                    List<Map<String,Object>> l = Objects.get(Objects.fromWire(JSONFormat.evalJSON(IOUtils.toString(zis,"UTF-8"))),"list");
                    list.addAll(table.doImport(l));
                }
            }
        }catch (IOException e){
            throw S1SystemError.wrap(e);
        }catch (JSONFormatException e){
            throw S1SystemError.wrap(e);
        }
    }

    @Override
    protected void setFileMeta(FileStorage.FileMetaBean meta) {
        meta.setContentType("application/zip");
        meta.setExt("zip");
    }

    private ZipOutputStream zos;
    private ByteArrayOutputStream os;

    @Override
    protected void prepareExport(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) {
        try{
            os = new ByteArrayOutputStream();
            zos = new ZipOutputStream(os);

        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

    @Override
    protected void addPortionToExport(int i, List<Map<String, Object>> list) {
        try{
            zos.putNextEntry(new ZipEntry("list_"+i+".json"));
            zos.write(JSONFormat.toJSON(
                    Objects.toWire(Objects.newHashMap(String.class,Object.class,"list",list))).getBytes("UTF-8"));
            zos.closeEntry();
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

    @Override
    protected void finishExport(int files, long count) {
        try{
            zos.putNextEntry(new ZipEntry("info.json"));
            zos.write(JSONFormat.toJSON(
                    Objects.toWire(Objects.newHashMap(String.class,Object.class,"count",count,"files",files))).getBytes("UTF-8"));
            zos.closeEntry();
            zos.flush();
            zos.close();
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

    @Override
    protected void writeExport(OutputStream os) {
        try {
            os.write(this.os.toByteArray());
        } catch (IOException e) {
            throw S1SystemError.wrap(e);
        }
    }
}
