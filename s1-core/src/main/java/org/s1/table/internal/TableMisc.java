package org.s1.table.internal;

import org.s1.S1SystemError;
import org.s1.cluster.Session;
import org.s1.cluster.datasource.MoreThanOneFoundException;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.objects.ObjectPath;
import org.s1.objects.Objects;
import org.s1.objects.schema.ListAttribute;
import org.s1.objects.schema.MapAttribute;
import org.s1.objects.schema.ObjectSchemaAttribute;
import org.s1.table.ActionBean;
import org.s1.table.ActionNotAvailableException;
import org.s1.table.StateBean;
import org.s1.table.Table;
import org.s1.table.format.FieldQueryNode;
import org.s1.table.format.Query;
import org.s1.user.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base table class
 */
class TableMisc {

    private static final Logger LOG = LoggerFactory.getLogger(TableMisc.class);

    private TableBase table;

    /**
     *
     * @param table
     */
    public TableMisc(TableBase table) {
        this.table = table;
    }

    /**
     * @param id
     * @return
     */
    String getLockName(String id) {
        return "Table:" + table.getCollection() + ":" + id;
    }

    /**
     * Get new id field value
     *
     * @return
     */
    String newId() {
        return table.getCollection() + "_" + UUID.randomUUID().toString();
    }

    /**
     * @param id
     * @return
     */
    List<ActionBean> getAvailableActions(String id) throws NotFoundException, AccessDeniedException {
        Map<String, Object> obj = null;
        if (id != null) {
            obj = table.get(id);
        }
        return table.getAvailableActionsForRecord(obj);
    }

    /**
     * @param record
     * @return
     */
    List<ActionBean> getAvailableActionsForRecord(Map<String, Object> record) {
        String state = null;
        if (record != null) {
            state = Objects.get(record, Table.STATE);
        }
        List<ActionBean> a = Objects.newArrayList();
        for (ActionBean it : table.getActions()) {
            if ((!Objects.isNullOrEmpty(state) && Objects.equals(state, it.getFrom()))
                    || (Objects.isNullOrEmpty(state) && Objects.isNullOrEmpty(it.getFrom()))) {
                //check access
                if (table.isActionAllowed(it, record))
                    a.add(it);
            }
        }
        return a;
    }

    /**
     *
     * @param id
     * @param name
     * @param record
     * @return
     * @throws org.s1.table.ActionNotAvailableException
     * @throws NotFoundException
     */
    ActionBean getAction(String id, String name, Map<String, Object> record) throws ActionNotAvailableException, NotFoundException {
        if(record==null)
            record = Objects.newHashMap();
        ActionBean action = null;
        for (ActionBean it : table.getActions()) {
            if (it.getName().equals(name)) {
                action = it;
                break;
            }
        }
        if (action == null)
            throw new S1SystemError("Action " + name + " not found in table " + table.getName());

        Map<String, Object> oldObject = null;
        if (!Objects.isNullOrEmpty(action.getFrom())) {
            Query search = new Query(new FieldQueryNode("id", FieldQueryNode.FieldOperation.EQUALS,id));
            table.prepareSearch(search);
            try {
                oldObject = table.collectionGet(table.getCollection(), search);
            } catch (MoreThanOneFoundException e) {
                throw S1SystemError.wrap(e);
            }
            record.clear();
            record.putAll(oldObject);
            //get(id);
        }
        List<ActionBean> actions = getAvailableActionsForRecord(oldObject);
        action = null;
        for (ActionBean it : actions) {
            if (it.getName().equals(name)) {
                action = it;
                break;
            }
        }
        if (action == null)
            throw new ActionNotAvailableException("Action " + name + " is not allowed for "
                    + Session.getSessionBean().getUserId()
                    + " in table " + table.getName());

        return action;
    }

    /**
     * @param name
     * @return
     */
    public StateBean getStateByName(String name) {
        if (name == null)
            return null;
        StateBean state = null;
        for (StateBean it : table.getStates()) {
            if (name.equals(it.getName())) {
                state = it;
                break;
            }
        }
        if (!Objects.isNullOrEmpty(name) && state == null)
            throw new S1SystemError("State " + name + " not found in table " + table.getName());
        return state;
    }

    /**
     *
     * @param path
     * @return
     */
    protected String getAttributeLabel(String path) {
        String label = "";
        try {
            String[] ps = ObjectPath.tokenizePath(path);
            ObjectSchemaAttribute a = table.getSchema().getRootMapAttribute();
            int i = 0;
            for (String p : ps) {
                i++;
                String l = ObjectPath.getLocalName(p);
                int[] j = ObjectPath.getNumber(p);
                for (ObjectSchemaAttribute _a : ((MapAttribute) a).getAttributes()) {
                    if (_a.getName().equals(l)) {
                        a = _a;
                        break;
                    }
                }
                label += a.getLabel();
                if (j != null && j.length > 0) {
                    for (int k : j) {
                        a = ((ListAttribute) a).getList().get(k);
                        label += "[" + k + "]";
                    }
                }
                if (i < ps.length)
                    label += " / ";
            }
        } catch (Throwable e) {
            label = path;
        }
        return label;
    }

}
