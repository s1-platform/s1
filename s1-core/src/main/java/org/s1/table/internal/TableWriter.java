package org.s1.table.internal;

import org.s1.S1SystemError;
import org.s1.cluster.Locks;
import org.s1.cluster.datasource.AlreadyExistsException;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.cluster.node.Transactions;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;
import org.s1.objects.schema.ObjectSchemaValidationException;
import org.s1.table.ActionBean;
import org.s1.table.ActionNotAvailableException;
import org.s1.table.StateBean;
import org.s1.table.Table;
import org.s1.user.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Base table class
 */
class TableWriter {

    private static final Logger LOG = LoggerFactory.getLogger(TableWriter.class);

    private TableBase table;

    /**
     *
     * @param table
     */
    public TableWriter(TableBase table) {
        this.table = table;
    }

    /**
     *
     * @param id
     * @param action
     * @param data
     * @param foundation
     * @return
     * @throws AccessDeniedException
     * @throws ObjectSchemaValidationException
     * @throws org.s1.table.ActionNotAvailableException
     * @throws AlreadyExistsException
     * @throws NotFoundException
     */
    public Map<String, Object> changeState(final String id, final String action,
                                           final Map<String, Object> data, final Map<String, Object> foundation)
            throws AccessDeniedException, ObjectSchemaValidationException, ActionNotAvailableException, AlreadyExistsException, NotFoundException {
        table.checkAccess();
        try {
            return Transactions.run(new Closure<String, Map<String, Object>>() {
                @Override
                public Map<String, Object> call(String input) throws ClosureException {
                    try {
                        if(Objects.isNullOrEmpty(id)){
                            //add
                            return table.changeRecordState(id, action, data, foundation);
                        }else{
                            //lock and set
                            return (Map<String, Object>) Locks.waitAndRun(table.getLockName(id), new Closure<String, Object>() {
                                @Override
                                public Object call(String input) throws ClosureException {
                                    try {
                                        return table.changeRecordState(id, action, data, foundation);
                                    } catch (Throwable e) {
                                        throw ClosureException.wrap(e);
                                    }
                                }
                            }, Table.LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
                        }
                    }catch (Throwable e){
                        if(e instanceof ClosureException)
                            throw (ClosureException)e;
                        throw ClosureException.wrap(e);
                    }
                }
            });
        } /*catch (TimeoutException e) {
            throw S1SystemError.wrap(e);
        } */catch (ClosureException e) {
            if (e.getCause() != null) {
                if (e.getCause() instanceof ObjectSchemaValidationException) {
                    throw (ObjectSchemaValidationException) e.getCause();
                } else if (e.getCause() instanceof ActionNotAvailableException) {
                    throw (ActionNotAvailableException) e.getCause();
                } else if (e.getCause() instanceof AlreadyExistsException) {
                    throw (AlreadyExistsException) e.getCause();
                } else if (e.getCause() instanceof NotFoundException) {
                    throw (NotFoundException) e.getCause();
                }
            }
            throw e.toSystemError();
        }
    }

    /**
     *
     * @param id
     * @param action
     * @param data
     * @param foundation
     * @return
     * @throws ObjectSchemaValidationException
     * @throws ActionNotAvailableException
     * @throws AlreadyExistsException
     * @throws NotFoundException
     * @throws ClosureException
     * @throws AccessDeniedException
     */
    protected Map<String, Object> changeRecordState(String id, String action,
                                                    Map<String, Object> data, Map<String, Object> foundation)
            throws ObjectSchemaValidationException, ActionNotAvailableException, AlreadyExistsException, NotFoundException, ClosureException, AccessDeniedException {
        if (data == null)
            data = Objects.newHashMap();

        Map<String, Object> oldObject = Objects.newHashMap();
        ActionBean a = table.getAction(id, action, oldObject);
        final boolean isFrom = !Objects.isNullOrEmpty(a.getFrom());
        final boolean isTo = !Objects.isNullOrEmpty(a.getTo());
        if(!isFrom){
            //add
            oldObject = null;
            if(Objects.isNullOrEmpty(id))
                id = table.newId();
        }

        //state
        StateBean state = table.getStateByName(a.getTo());

        //validate data
        if (a.getSchema() != null)
            data = a.getSchema().validate(data);

        //validate foundation
        if (a.isLog() && isTo) {
            if (foundation == null)
                foundation = Objects.newHashMap();
            //validate foundation
            if (a.getFoundationSchema() != null)
                foundation = a.getFoundationSchema().validate(foundation);
        }

        Map<String, Object> newObject = null;
        if(isTo){
            newObject = Objects.newHashMap("id",id,Table.STATE,state.getName());
            if(isFrom)
                newObject = Objects.copy(oldObject);
            //merge
            newObject = merge(a, newObject, data);

            //validate
            if (state.getSchema() != null)
                newObject = state.getSchema().validate(newObject);
            newObject = table.getSchema().validate(newObject);

            newObject.put(Table.STATE, state.getName());
            newObject.put("id", id);
        }

        //brules
        runBefore(a, oldObject, data, foundation);

        if(isTo){
            final Map<String, Object> _newObject = newObject;
            final String _id = id;
            try {
                Locks.waitAndRun(table.getLockName(null), new Closure<String, Object>() {
                    @Override
                    public Object call(String input) throws ClosureException {
                        try {
                            table.checkUnique(_newObject, !isFrom);
                            //save
                            if(isFrom)
                                table.collectionSet(table.getCollection(), _id, _newObject);
                            else
                                table.collectionAdd(table.getCollection(), _newObject);
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
        }else{
            table.collectionRemove(table.getCollection(), id);
            table.collectionAdd(table.getCollection() + Table.TRASH_SUFFIX, oldObject);
        }

        //add log
        if(a.isLog() && isTo)
            table.log(id, a, oldObject, newObject, data, foundation);

        runAfter(a, oldObject, newObject, data, foundation);

        return isTo?newObject:oldObject;
    }

    /**
     * @param action
     * @param object
     * @param data
     * @return
     */
    protected Map<String, Object> merge(ActionBean action, Map<String, Object> object, Map<String, Object> data) {
        Map<String, Object> result = Objects.merge(Objects.newHashMap(String.class, Object.class), object, data);
        if (action.getMerge() != null) {
            action.getMerge().callQuite(new ActionBean.MergeBean(result, object, data));
        }
        return result;
    }

    /**
     *
     * @param action
     * @param oldObject
     * @param data
     * @param foundation
     * @throws ClosureException
     */
    protected void runBefore(ActionBean action,
                             Map<String, Object> oldObject,
                             Map<String, Object> data, Map<String, Object> foundation) throws ClosureException {
        if (action.getBefore() != null) {
            action.getBefore().call(new ActionBean.BeforeBean(data, foundation, action, oldObject));
        }
    }

    /**
     *
     * @param action
     * @param oldObject
     * @param newObject
     * @param data
     * @param foundation
     * @throws ClosureException
     */
    protected void runAfter(ActionBean action,
                            Map<String, Object> oldObject, Map<String, Object> newObject,
                            Map<String, Object> data, Map<String, Object> foundation) throws ClosureException {
        if (action.getAfter() != null) {
            action.getAfter().call(new ActionBean.AfterBean(data, foundation, action, oldObject, newObject));
        }
    }

}
