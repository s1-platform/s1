package org.s1.table.internal;

import org.s1.cluster.Session;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.objects.ObjectDiff;
import org.s1.objects.ObjectPath;
import org.s1.objects.Objects;
import org.s1.objects.schema.ListAttribute;
import org.s1.objects.schema.MapAttribute;
import org.s1.objects.schema.ObjectSchemaAttribute;
import org.s1.table.ActionBean;
import org.s1.table.Table;
import org.s1.table.format.FieldQueryNode;
import org.s1.table.format.GroupQueryNode;
import org.s1.table.format.Query;
import org.s1.table.format.Sort;
import org.s1.user.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Table log
 */
class TableHistory {

    private static final Logger LOG = LoggerFactory.getLogger(TableHistory.class);

    private TableBase table;

    /**
     *
     * @param table
     */
    public TableHistory(TableBase table) {
        this.table = table;
    }

    /**
     *
     * @param result
     * @param id
     * @param search
     * @param skip
     * @param max
     * @return
     * @throws org.s1.cluster.datasource.NotFoundException
     * @throws org.s1.user.AccessDeniedException
     */
    public long listLog(List<Map<String, Object>> result, String id, Query search,
                        int skip, int max) throws NotFoundException, AccessDeniedException {
        table.checkAccess();
        Map<String, Object> oldRecord = table.get(id);
        table.checkLogAccess(oldRecord);
        if(search==null)
            search = new Query();

        search.setNode(new GroupQueryNode(GroupQueryNode.GroupOperation.AND,
                new FieldQueryNode("record", FieldQueryNode.FieldOperation.EQUALS,id),
                search.getNode()
        ));
        long count = table.collectionList(table.getCollection() + Table.HISTORY_SUFFIX,
                result, null, search, new Sort("date",true), null, skip, max);
        return count;
    }

    /**
     *
     * @param id
     * @param action
     * @param oldObject
     * @param newObject
     * @param data
     * @param foundation
     */
    protected void log(String id, ActionBean action, Map<String, Object> oldObject, Map<String, Object> newObject,
                       Map<String, Object> data, Map<String, Object> foundation) {
        List<Map<String, Object>> changes = null;
        if(newObject!=null && oldObject!=null){
            List<ObjectDiff.DiffBean> diff = Objects.diff(oldObject, newObject);
            changes = Objects.newArrayList();
            for (ObjectDiff.DiffBean b : diff) {
                changes.add(Objects.newHashMap(String.class, Object.class,
                        "label", table.getAttributeLabel(b.getPath()),
                        "new", b.getNewValue(),
                        "old", b.getOldValue(),
                        "path", b.getPath()
                ));
            }
        }
        Map<String, Object> h = Objects.newHashMap(
                "action", Objects.newHashMap(
                "from", action.getFrom(),
                "to", action.getTo(),
                "name", action.getName(),
                "label", action.getLabel(),
                "schema", action.getSchema() == null ? null : action.getSchema().toMap(),
                "foundationSchema", action.getFoundationSchema() == null ? null : action.getFoundationSchema().toMap()
        ),
                "id", UUID.randomUUID().toString(),
                "record", id,
                "date", new Date(),
                "user", Session.getSessionBean().getUserId(),
                "foundation", foundation,
                "new", newObject,
                "old", oldObject,
                "changes", changes,
                "data", data
        );
        table.collectionAdd(table.getCollection() + Table.HISTORY_SUFFIX, h);
    }

}
