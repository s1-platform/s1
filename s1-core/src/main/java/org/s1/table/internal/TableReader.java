package org.s1.table.internal;

import org.s1.S1SystemError;
import org.s1.cluster.datasource.MoreThanOneFoundException;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.objects.Objects;
import org.s1.objects.schema.ObjectSchemaValidationException;
import org.s1.table.AggregationBean;
import org.s1.table.CountGroupBean;
import org.s1.table.EnrichBean;
import org.s1.table.Table;
import org.s1.table.format.*;
import org.s1.user.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Table reader
 */
class TableReader {

    private static final Logger LOG = LoggerFactory.getLogger(TableReader.class);

    private TableBase table;

    /**
     *
     * @param table
     */
    public TableReader(TableBase table) {
        this.table = table;
    }

    /**
     *
     * @param result
     * @param fullTextQuery
     * @param search
     * @param sort
     * @param fields
     * @param skip
     * @param max
     * @param ctx
     * @return
     * @throws AccessDeniedException
     */
    public long list(List<Map<String, Object>> result, String fullTextQuery,
                     Query search, Sort sort, FieldsMask fields,
                     int skip, int max, Map<String, Object> ctx) throws AccessDeniedException {
        if (ctx == null)
            ctx = Objects.newHashMap();
        table.checkAccess();
        if (search == null)
            search = new Query();
        table.prepareSearch(search);

        if (sort == null)
            sort = new Sort();
        table.prepareSort(sort);

        long count = table.collectionList(table.getCollection(), result, fullTextQuery, search, sort, fields, skip, max);
        for (Map<String, Object> m : result) {
            try {
                table.enrichRecord(m, true, ctx);
            } catch (Exception e) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Error enrich: " + e.getMessage());
            }
        }
        return count;
    }

    /**
     *
     * @param id
     * @param ctx
     * @return
     * @throws NotFoundException
     * @throws AccessDeniedException
     */
    public Map<String, Object> get(String id, Map<String, Object> ctx) throws NotFoundException, AccessDeniedException {
        if (ctx == null)
            ctx = Objects.newHashMap();
        table.checkAccess();
        Query search = new Query(new FieldQueryNode("id", FieldQueryNode.FieldOperation.EQUALS,id));
        table.prepareSearch(search);
        Map<String, Object> m = null;
        try {
            m = table.collectionGet(table.getCollection(), search);
        } catch (MoreThanOneFoundException e) {
            throw S1SystemError.wrap(e);
        }
        try {
            table.enrichRecord(m, false, ctx);
        } catch (Exception e) {
            if (LOG.isDebugEnabled())
                LOG.warn("Error enrich: " + e.getMessage());
        }
        return m;
    }

    /**
     *
     * @param field
     * @param search
     * @return
     * @throws AccessDeniedException
     */
    public AggregationBean aggregate(String field, Query search) throws AccessDeniedException {
        table.checkAccess();
        if (search == null)
            search = new Query();
        table.prepareSearch(search);
        return table.collectionAggregate(table.getCollection(), field, search);
    }

    /**
     *
     * @param field
     * @param search
     * @return
     * @throws AccessDeniedException
     */
    public List<CountGroupBean> countGroup(String field, Query search) throws AccessDeniedException {
        table.checkAccess();
        if (search == null)
            search = new Query();
        table.prepareSearch(search);
        return table.collectionCountGroup(table.getCollection(), field, search);
    }

    /**
     *
     * @param record
     * @param list
     * @param ctx
     */
    protected void enrichRecord(Map<String, Object> record, boolean list, Map<String, Object> ctx) {
        if (table.getEnrichRecord() != null) {
            table.getEnrichRecord().callQuite(new EnrichBean(record, ctx, list));
            if (ctx.containsKey(Table.CTX_VALIDATE_KEY)) {
                boolean expand = Objects.get(Boolean.class, ctx, Table.CTX_EXPAND_KEY, false);
                boolean deep = Objects.get(Boolean.class, ctx, Table.CTX_DEEP_KEY, false);
                Map<String, Object> r = Objects.copy(record);
                try {
                    r = table.getSchema().validate(r, expand, deep, null);
                } catch (ObjectSchemaValidationException e) {
                    LOG.warn("Cannot validate data on table '" + table.getName() + "' schema: " + e.getMessage());
                }
                record.clear();
                record.putAll(r);
            }
        }
    }

    /**
     *
     * @param search
     */
    protected void prepareSearch(Query search) {
        if (table.getPrepareSearch() != null) {
            table.getPrepareSearch().callQuite(search);
        }
    }

    /**
     *
     * @param s
     */
    protected void prepareSort(Sort s) {
        if (table.getPrepareSort() != null) {
            table.getPrepareSort().callQuite(s);
        }
    }

}
