package org.s1.table.internal;

import org.s1.S1SystemError;
import org.s1.cluster.Locks;
import org.s1.cluster.datasource.AlreadyExistsException;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;
import org.s1.objects.schema.ObjectSchemaValidationException;
import org.s1.table.ImportBean;
import org.s1.table.StateBean;
import org.s1.table.Table;
import org.s1.user.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Table import
 */
class TableImporter {

    private static final Logger LOG = LoggerFactory.getLogger(TableImporter.class);

    private TableBase table;

    /**
     *
     * @param table
     */
    public TableImporter(TableBase table) {
        this.table = table;
    }

    /**
     * @param list
     * @return
     */
    public List<Map<String, Object>> doImport(List<Map<String, Object>> list) throws AccessDeniedException {
        List<Map<String, Object>> res = Objects.newArrayList();
        if (table.getImportAction() == null)
            throw new S1SystemError("Import action undefined for table " + table.getName());
        for (Map<String, Object> element : list) {
            try {
                Map<String, Object> oldObject = null;
                String state = null;
                String id = Objects.get(element, "id");
                if (id != null) {
                    try {
                        oldObject = table.get(id);
                        state = Objects.get(oldObject, Table.STATE);
                    } catch (NotFoundException e) {
                    }
                } else {
                    id = table.newId();
                    element.put("id", id);
                }
                if (table.getImportSchema() != null)
                    element = table.getImportSchema().validate(element, Objects.newHashMap(String.class, Object.class, "record", oldObject));

                final Map<String, Object> _element = element;
                final String _id = id;
                final String _state = state;
                final Map<String, Object> _oldObject = oldObject;
                Locks.waitAndRun(table.getLockName(id), new Closure<String, Object>() {
                    @Override
                    public Object call(String input) throws ClosureException {
                        try {
                            table.importRecord(_id, _oldObject, _state, _element);
                        } catch (Throwable e) {
                            throw ClosureException.wrap(e);
                        }
                        return null;
                    }
                }, Table.LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
                res.add(Objects.newHashMap(String.class, Object.class, "success", true, "id", id));
            } catch (Throwable e) {
                if (e instanceof ClosureException && e.getCause() != null)
                    e = e.getCause();
                LOG.info("Import error: " + e.getMessage());
                LOG.debug("Import error", e);
                res.add(Objects.newHashMap(String.class, Object.class, "success", false, "message", e.getMessage(), "class", e.getClass().getName()));
            }
        }
        return res;
    }

    /**
     * @param id
     * @param oldObject
     * @param state
     * @param data
     */
    public void importRecord(final String id, final Map<String, Object> oldObject, final String state, final Map<String, Object> data)
            throws ObjectSchemaValidationException, AlreadyExistsException {
        Map<String, Object> newObject = Objects.newHashMap("id", id);
        newObject = Objects.merge(newObject,oldObject,data);
        newObject.put("id",id);
        newObject.put(Table.STATE,state);
        table.getImportAction().callQuite(new ImportBean(id, newObject, oldObject, state, data));

        if (!newObject.containsKey(Table.STATE)) {
            throw new S1SystemError("New object must contain _state field after import action (table: " + table.getName() + ")");
        }
        StateBean st = table.getStateByName(Objects.get(String.class, newObject, Table.STATE));
        if(st==null){
            throw new S1SystemError("Table import: _state field must be not null (set it for newRecord in importAction)");
        }
        //validate state
        if (st.getSchema() != null)
            newObject = st.getSchema().validate(newObject);
        //validate
        newObject = table.getSchema().validate(newObject);

        final Map<String, Object> _newObject = newObject;
        try {
            Locks.waitAndRun(table.getLockName(null), new Closure<String, Object>() {
                @Override
                public Object call(String input) throws ClosureException {
                    try {
                        //unique
                        table.checkUnique(_newObject, oldObject == null);

                        //save
                        if (oldObject != null) {
                            //set
                            table.collectionSet(table.getCollection(), id, _newObject);
                        } else {
                            //add
                            table.collectionAdd(table.getCollection(), _newObject);
                        }
                    } catch (Throwable e) {
                        throw ClosureException.wrap(e);
                    }
                    return null;
                }
            }, Table.LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw S1SystemError.wrap(e);
        } catch (ClosureException e) {
            if (e.getCause() != null) {
                if (e.getCause() instanceof AlreadyExistsException) {
                    throw (AlreadyExistsException) e.getCause();
                }
            }
            throw e.toSystemError();
        }
    }
}
