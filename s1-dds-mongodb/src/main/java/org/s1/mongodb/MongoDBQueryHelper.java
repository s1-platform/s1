package org.s1.mongodb;

import com.mongodb.*;
import org.bson.types.ObjectId;
import org.s1.cluster.datasource.AlreadyExistsException;
import org.s1.cluster.datasource.MoreThanOneFoundException;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MongoDB Query helper
 * DEBUG - results
 */
public class MongoDBQueryHelper {

    private static final Logger LOG = LoggerFactory.getLogger(MongoDBQueryHelper.class);

    /**
     * Select exactly one record from collection
     *
     * @param instance
     * @param collection
     * @param search
     * @throws NotFoundException
     * @throws MoreThanOneFoundException
     * @return
     */
    public static Map<String, Object> get(String instance, String collection, Map<String,Object> search)
    throws NotFoundException, MoreThanOneFoundException {
        System.out.println("==="+MongoDBFormat.fromMap(search));
        ensureOnlyOne(instance, collection, search);

        DBCollection coll = MongoDBConnectionHelper.getConnection(instance).getCollection(collection);
        Map<String,Object> m = MongoDBFormat.toMap(coll.findOne(MongoDBFormat.fromMap(search)));
        if(LOG.isDebugEnabled())
            LOG.debug("MongoDB get result (collection:"+collection+",search:"+search+"\n\t> "+m);
        return m;
    }

    public static final String SCORE = "_score";

    /**
     *
     * @param res
     * @param instance
     * @param collection
     * @param fullTextQuery
     * @param search
     * @param fields
     * @param skip
     * @param max
     * @return
     */
    public static long fullTextSearch(List<Map<String, Object>> res, String instance, String collection,
                                      String fullTextQuery,
                                      Map<String,Object> search,
                                      Map<String,Object> fields, int skip, int max) {
        DB db = MongoDBConnectionHelper.getConnection(instance);
        BasicDBObject request = new BasicDBObject();
        request.put("text", collection);
        request.put("search", fullTextQuery);
        //comment to get valid count
        //request.put("limit", max + skip);
        request.put("filter", MongoDBFormat.fromMap(search));
        request.put("project", Objects.newHashMap("_id", 1));
        CommandResult cr = db.command(request);
        List<Map<String,Object>> l = Objects.get(cr,"results",new ArrayList<Map<String, Object>>());
        int i=0;
        for(Map<String,Object> m:l){
            if(++i<skip)
                continue;
            Map<String,Object> _m = Objects.newHashMap();
            DBObject o = db.getCollection(collection).findOne(new BasicDBObject("_id",Objects.get(m,"obj._id")),
                    MongoDBFormat.fromMap(fields));
            if(o!=null){
                _m.putAll(MongoDBFormat.toMap(o));
                res.add(_m);
            }
            _m.put(SCORE,Objects.get(m,"score"));
            if(i>=skip+max)
                break;
        }
        long cnt = Objects.get(Long.class,cr,"stats.n",0L);

        if(LOG.isDebugEnabled())
            LOG.debug("MongoDB list result (collection:"+collection+", search:"+search+", full-text:"+fullTextQuery+", fields:"+fields+", max:"+max+", skip:"+skip +
                    "\n\t>command result: "+cr +
                    "\n\t>count: "+cnt +
                    "\n\t> "+res);

        return cnt;
    }

    /**
     *
     * @param res
     * @param instance
     * @param collection
     * @param search
     * @param sort
     * @param fields
     * @param skip
     * @param max
     * @return
     */
    public static long list(List<Map<String, Object>> res, String instance, String collection,
                            Map<String,Object> search, Map<String,Object> sort,
                            Map<String,Object> fields, int skip, int max) {
        return list(res, instance, collection,search,sort,fields,skip,max,null);
    }

    /**
     *
     * @param res
     * @param instance
     * @param collection
     * @param search
     * @param sort
     * @param fields
     * @param skip
     * @param max
     * @param prepareCursor
     * @return
     */
    public static long list(List<Map<String, Object>> res, String instance, String collection,
                            Map<String,Object> search, Map<String,Object> sort,
                            Map<String,Object> fields, int skip, int max, Closure<DBCursor,DBCursor> prepareCursor) {
        DBCollection coll = MongoDBConnectionHelper.getConnection(instance).getCollection(collection);

        if (search == null)
            search = Objects.newHashMap();

        DBCursor cur = coll.find(MongoDBFormat.fromMap(search), MongoDBFormat.fromMap(fields));
        if (max > 0)
            cur.limit(max);
        if (skip >= 0)
            cur.skip(skip);

        if (sort != null){
            cur = cur.sort(MongoDBFormat.fromMap(sort));
        }

        if(prepareCursor!=null){
            cur = prepareCursor.callQuite(cur);
        }

        while (cur.hasNext()) {
            DBObject obj = cur.next();

            Map<String,Object> m = MongoDBFormat.toMap(obj);

            res.add(m);
        }

        long cnt = cur.getCollection().count(cur.getQuery());

        if(LOG.isDebugEnabled())
            LOG.debug("MongoDB list result (collection:"+cur.getCollection()+", search:"+search+", sort:"+sort+", fields:"+fields+", max:"+max+", skip:"+skip +
                    "\n\t>count: "+cnt +
                    "\n\t> "+res);

        return cnt;
    }

    /**
     *
     * @param instance
     * @param collection
     * @param search
     * @throws AlreadyExistsException
     */
    public static void ensureNotExists(String instance, String collection, Map<String,Object> search) throws AlreadyExistsException {
        DBCollection coll = MongoDBConnectionHelper.getConnection(instance).getCollection(collection);
        if(search==null)
            search = Objects.newHashMap();
        long cnt = coll.count(MongoDBFormat.fromMap(search));
        if(cnt>0){
            if(LOG.isDebugEnabled())
                LOG.debug("Record already exists: "+collection+", search: "+search);
            throw new AlreadyExistsException("MongoDB, instance: "+instance+", collection: "+collection+", search: "+search);
        }
    }

    /**
     *
     * @param instance
     * @param collection
     * @param search
     * @throws NotFoundException
     * @throws MoreThanOneFoundException
     */
    public static void ensureOnlyOne(String instance, String collection, Map<String,Object> search)
            throws NotFoundException, MoreThanOneFoundException{
        DBCollection coll = MongoDBConnectionHelper.getConnection(instance).getCollection(collection);
        if(search==null)
            search = Objects.newHashMap();
        long cnt = coll.count(MongoDBFormat.fromMap(search));
        if(cnt==0){
            if(LOG.isDebugEnabled())
                LOG.debug("Record not found instance: "+instance+", collection: "+collection+", search: "+search);
            throw new NotFoundException("MongoDB, instance: "+instance+", collection: "+collection+", search: "+search);
        }
        if(cnt>1){
            if(LOG.isDebugEnabled())
                LOG.debug("More than one record found ("+cnt+") found instance: "+instance+", collection: "+collection+", search: "+search);
            throw new MoreThanOneFoundException("MongoDB, instance: "+instance+", collection: "+collection+", search: "+search);
        }
    }

}
