package org.s1.table.web;

import org.s1.cluster.datasource.FileStorage;
import org.s1.objects.Objects;
import org.s1.objects.schema.ObjectSchema;
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

    /**
     *
     * @param list
     * @param file
     * @return
     */
    protected abstract PreviewBean preview(FileStorage.FileReadBean file);

    /**
     *
     * @param list
     * @param file
     * @param table
     */
    protected abstract void doImport(List<Map<String,Object>> list, FileStorage.FileReadBean file, Table table)
        throws AccessDeniedException;

    protected abstract void setFileMeta(FileStorage.FileMetaBean meta);

    protected abstract void prepareExport(ObjectSchema schema, Map<String, Object> params, HttpServletRequest request, HttpServletResponse response);

    protected abstract void addPortionToExport(int i, List<Map<String,Object>> list);

    protected abstract void finishExport(int files, long count);

    protected abstract void writeExport(OutputStream os);


    public static class PreviewBean{
        private ObjectSchema schema;
        private long count=0;
        private List<Map<String,Object>> list = Objects.newArrayList();

        public PreviewBean(ObjectSchema schema, long count, List<Map<String,Object>> list) {
            this.schema = schema;
            this.count = count;
            this.list = list;
        }

        /**
         *
         * @return
         */
        public ObjectSchema getSchema() {
            return schema;
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
