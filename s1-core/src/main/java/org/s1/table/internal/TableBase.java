package org.s1.table.internal;

import org.s1.cluster.Session;
import org.s1.cluster.datasource.AlreadyExistsException;
import org.s1.cluster.datasource.MoreThanOneFoundException;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;
import org.s1.objects.schema.ObjectSchema;
import org.s1.objects.schema.ObjectSchemaValidationException;
import org.s1.table.*;
import org.s1.table.format.FieldsMask;
import org.s1.table.format.Query;
import org.s1.table.format.Sort;
import org.s1.user.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Base table class
 */
public abstract class TableBase {

    private static final Logger LOG = LoggerFactory.getLogger(TableBase.class);

    final TableDescriptor descriptor = new TableDescriptor(this);
    final TableAccessControler accessControler = new TableAccessControler(this);
    final TableImporter importer = new TableImporter(this);
    final TableIndexer indexer = new TableIndexer(this);
    final TableReader reader = new TableReader(this);
    final TableMisc misc = new TableMisc(this);
    final TableWriter writer = new TableWriter(this);
    final TableHistory history = new TableHistory(this);


    /**
     * LOCK TIMEOUT
     */
    public static int LOCK_TIMEOUT = 30000;

    public static final String STATE = "_state";

    public static final String CTX_VALIDATE_KEY = "validate";
    public static final String CTX_DEEP_KEY = "deep";
    public static final String CTX_EXPAND_KEY = "expand";

    public static final String HISTORY_SUFFIX = "_history";
    public static final String TRASH_SUFFIX = "_removed";

    public static final String SCRIPT_ENGINE_PATH = "table.scriptEngine";

    /**
     *
     */
    public void init() {
        //indexes
        indexer.checkIndexes();
    }


    /*==========================================
     * DESCRIPTOR
     ==========================================*/

    public List<ActionBean> getActions() {
        return descriptor.getActions();
    }

    public String getName() {
        return descriptor.getName();
    }

    public void fromMap(Map<String, Object> m) {
        descriptor.fromMap(m);
    }

    public ObjectSchema getSchema() {
        return descriptor.getSchema();
    }

    public void setName(String name) {
        descriptor.setName(name);
    }

    public void setCollection(String collection) {
        descriptor.setCollection(collection);
    }

    public Closure<Sort, Object> getPrepareSort() {
        return descriptor.getPrepareSort();
    }

    public Closure<ImportBean, Object> getImportAction() {
        return descriptor.getImportAction();
    }

    public Closure<Object, Boolean> getAccess() {
        return descriptor.getAccess();
    }

    public void setEnrichRecord(Closure<EnrichBean, Object> enrichRecord) {
        descriptor.setEnrichRecord(enrichRecord);
    }

    public ObjectSchema getImportSchema() {
        return descriptor.getImportSchema();
    }

    public Closure<Object, Boolean> getImportAccess() {
        return descriptor.getImportAccess();
    }

    public void setImportSchema(ObjectSchema importSchema) {
        descriptor.setImportSchema(importSchema);
    }

    public void setImportAccess(Closure<Object, Boolean> importAccess) {
        descriptor.setImportAccess(importAccess);
    }

    public Closure<EnrichBean, Object> getEnrichRecord() {
        return descriptor.getEnrichRecord();
    }

    public void setImportAction(Closure<ImportBean, Object> importAction) {
        descriptor.setImportAction(importAction);
    }

    public void setPrepareSort(Closure<Sort, Object> prepareSort) {
        descriptor.setPrepareSort(prepareSort);
    }

    public void setLogAccess(Closure<Map<String, Object>, Boolean> logAccess) {
        descriptor.setLogAccess(logAccess);
    }

    public void setPrepareSearch(Closure<Query, Object> prepareSearch) {
        descriptor.setPrepareSearch(prepareSearch);
    }

    public List<StateBean> getStates() {
        return descriptor.getStates();
    }

    public void setSchema(ObjectSchema schema) {
        descriptor.setSchema(schema);
    }

    public String getCollection() {
        return descriptor.getCollection();
    }

    public Closure<Query, Object> getPrepareSearch() {
        return descriptor.getPrepareSearch();
    }

    public List<IndexBean> getIndexes() {
        return descriptor.getIndexes();
    }

    public Closure<Map<String, Object>, Boolean> getLogAccess() {
        return descriptor.getLogAccess();
    }

    public void setAccess(Closure<Object, Boolean> access) {
        descriptor.setAccess(access);
    }

    /*==========================================
     * INDEXES
     ==========================================*/

    /**
     *
     * @param collection
     * @param name
     * @param ind
     * @return
     */
    protected abstract void collectionIndex(String collection, String name, IndexBean ind);

    public List<IndexBean> getLogIndexes() {
        return indexer.getLogIndexes();
    }

    public void checkUnique(Map<String, Object> object, boolean isNew) throws AlreadyExistsException {
        indexer.checkUnique(object, isNew);
    }

    public void checkIndexes() {
        indexer.checkIndexes();
    }

    /*==========================================
     * READ
     ==========================================*/

    /**
     *
     * @param collection
     * @param result
     * @param fullTextQuery
     * @param search
     * @param sort
     * @param fields
     * @param skip
     * @param max
     * @return
     */
    protected abstract long collectionList(String collection, List<Map<String, Object>> result, String fullTextQuery,
                                           Query search, Sort sort, FieldsMask fields,
                                           int skip, int max);

    /**
     *
     * @param collection
     * @param search
     * @return
     * @throws org.s1.cluster.datasource.NotFoundException
     * @throws org.s1.cluster.datasource.MoreThanOneFoundException
     */
    protected abstract Map<String, Object> collectionGet(String collection, Query search) throws NotFoundException, MoreThanOneFoundException;

    /**
     *
     * @param collection
     * @param field
     * @param search
     * @return
     */
    protected abstract AggregationBean collectionAggregate(String collection, String field, Query search);

    /**
     *
     * @param collection
     * @param field
     * @param search
     * @return
     */
    protected abstract List<CountGroupBean> collectionCountGroup(String collection, String field, Query search);

    public Map<String, Object> get(String id) throws NotFoundException, AccessDeniedException {
        return get(id, null);
    }

    public long list(List<Map<String, Object>> result, String fullTextQuery, Query search, Sort sort, FieldsMask fields, int skip, int max) throws AccessDeniedException {
        return list(result, fullTextQuery, search, sort, fields, skip, max, null);
    } 
    public long list(List<Map<String, Object>> result, String fullTextQuery, Query search, Sort sort, FieldsMask fields, int skip, int max, Map<String, Object> ctx) throws AccessDeniedException {
        return reader.list(result, fullTextQuery, search, sort, fields, skip, max, ctx);
    }

    public Map<String, Object> get(String id, Map<String, Object> ctx) throws NotFoundException, AccessDeniedException {
        return reader.get(id, ctx);
    }

    public AggregationBean aggregate(String field, Query search) throws AccessDeniedException {
        return reader.aggregate(field, search);
    }

    public List<CountGroupBean> countGroup(String field, Query search) throws AccessDeniedException {
        return reader.countGroup(field, search);
    }

    public void enrichRecord(Map<String, Object> record, boolean list, Map<String, Object> ctx) {
        reader.enrichRecord(record, list, ctx);
    }

    public void prepareSearch(Query search) {
        reader.prepareSearch(search);
    }

    public void prepareSort(Sort s) {
        reader.prepareSort(s);
    }

    /*==========================================
     * WRITER
     ==========================================*/

    /**
     *
     * @param collection
     * @param data
     * @return
     */
    protected abstract void collectionAdd(String collection, Map<String, Object> data);

    /**
     *
     * @param collection
     * @param id
     * @param data
     * @return
     */
    protected abstract void collectionSet(String collection, String id, Map<String, Object> data);

    /**
     *
     * @param collection
     * @param id
     * @return
     */
    protected abstract void collectionRemove(String collection, String id);

    public Map<String, Object> changeState(String id, String action, Map<String, Object> data, Map<String, Object> foundation) throws AccessDeniedException, ObjectSchemaValidationException, ActionNotAvailableException, AlreadyExistsException, NotFoundException {
        return writer.changeState(id, action, data, foundation);
    }

    protected Map<String, Object> merge(ActionBean action, Map<String, Object> object, Map<String, Object> data) {
        return writer.merge(action, object, data);
    }

    protected void runAfter(ActionBean action, Map<String, Object> oldObject, Map<String, Object> newObject, Map<String, Object> data, Map<String, Object> foundation) throws ClosureException {
        writer.runAfter(action, oldObject, newObject, data, foundation);
    }

    protected void runBefore(ActionBean action, Map<String, Object> oldObject, Map<String, Object> data, Map<String, Object> foundation) throws ClosureException {
        writer.runBefore(action, oldObject, data, foundation);
    }

    protected Map<String, Object> changeRecordState(String id, String action, Map<String, Object> data, Map<String, Object> foundation) throws ObjectSchemaValidationException, ActionNotAvailableException, AlreadyExistsException, NotFoundException, ClosureException, AccessDeniedException {
        return writer.changeRecordState(id, action, data, foundation);
    }

    /*==========================================
     * HISTORY
     ==========================================*/

    public long listLog(List<Map<String, Object>> result, String id, Query search, int skip, int max) throws NotFoundException, AccessDeniedException {
        return history.listLog(result, id, search, skip, max);
    }

    protected void log(String id, ActionBean action, Map<String, Object> oldObject, Map<String, Object> newObject, Map<String, Object> data, Map<String, Object> foundation) {
        history.log(id, action, oldObject, newObject, data, foundation);
    }

    /*==========================================
     * IMPORT
     ==========================================*/

    public List<Map<String, Object>> doImport(List<Map<String, Object>> list) throws AccessDeniedException {
        return importer.doImport(list);
    }

    public void importRecord(String id, Map<String, Object> oldObject, String state, Map<String, Object> data) throws ObjectSchemaValidationException, AlreadyExistsException {
        importer.importRecord(id, oldObject, state, data);
    }

    /*==========================================
     * ACCESS CONTROL
     ==========================================*/

    /**
     * @throws org.s1.user.AccessDeniedException
     */
    public void checkAccess() throws AccessDeniedException {
        if (!isAccessAllowed())
            throw new AccessDeniedException("Access to " + getName() + " table is denied for: " + Session.getSessionBean().getUserId());
    }

    /**
     * @param record
     * @throws org.s1.user.AccessDeniedException
     */
    public void checkLogAccess(Map<String, Object> record) throws AccessDeniedException {
        if (!isLogAccessAllowed(record))
            throw new AccessDeniedException("Access to logs " + getName() + " table, #" + Objects.get(record, "id") + " is denied for: " + Session.getSessionBean().getUserId());
    }

    /**
     * @throws org.s1.user.AccessDeniedException
     */
    public void checkImportAccess() throws AccessDeniedException {
        if (!isImportAllowed())
            throw new AccessDeniedException("Import to " + getName() + " table is denied for: " + Session.getSessionBean().getUserId());
    }

    public boolean isActionAllowed(ActionBean action, Map<String, Object> record) {
        return accessControler.isActionAllowed(action, record);
    }

    public boolean isAccessAllowed() {
        return accessControler.isAccessAllowed();
    }

    public boolean isImportAllowed() {
        return accessControler.isImportAllowed();
    }

    public boolean isLogAccessAllowed(Map<String, Object> record) {
        return accessControler.isLogAccessAllowed(record);
    }

    /*==========================================
     * MISC
     ==========================================*/

    public String getLockName(String id) {
        return misc.getLockName(id);
    }

    public List<ActionBean> getAvailableActionsForRecord(Map<String, Object> record) {
        return misc.getAvailableActionsForRecord(record);
    }

    public StateBean getStateByName(String name) {
        return misc.getStateByName(name);
    }

    protected String newId() {
        return misc.newId();
    }

    public ActionBean getAction(String id, String name, Map<String, Object> record) throws ActionNotAvailableException, NotFoundException {
        return misc.getAction(id, name, record);
    }

    public List<ActionBean> getAvailableActions(String id) throws NotFoundException, AccessDeniedException {
        return misc.getAvailableActions(id);
    }

    public String getAttributeLabel(String path) {
        return misc.getAttributeLabel(path);
    }

}
