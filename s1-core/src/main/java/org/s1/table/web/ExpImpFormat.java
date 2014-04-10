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

import org.s1.cluster.dds.file.FileStorage;
import org.s1.objects.Objects;
import org.s1.objects.schema.ObjectSchema;
import org.s1.table.ImportResultBean;
import org.s1.table.Table;
import org.s1.user.AccessDeniedException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Table export import format
 */
public abstract class ExpImpFormat {

    protected abstract PreviewBean preview(FileStorage.FileReadBean file);

    protected abstract void doImport(List<ImportResultBean> list, FileStorage.FileReadBean file, Table table)
        throws AccessDeniedException;

    protected abstract void setFileMeta(FileStorage.FileMetaBean meta);

    protected abstract void prepareExport(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response);

    protected abstract void addPortionToExport(int i, List<Map<String,Object>> list);

    protected abstract void finishExport(int files, long count);

    protected abstract void writeExport(OutputStream os);


    public static class PreviewBean{
        private long count=0;
        private List<Map<String,Object>> list = Objects.newArrayList();

        public PreviewBean(long count, List<Map<String,Object>> list) {
            this.count = count;
            this.list = list;
        }

        /**
         *
         * @return
         */
        public long getCount() {
            return count;
        }

        /**
         *
         * @return
         */
        public List<Map<String, Object>> getList() {
            return list;
        }
    }
}
