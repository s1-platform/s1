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

package org.s1.mongodb;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.s1.cluster.dds.beans.CollectionId;
import org.s1.cluster.dds.beans.Id;
import org.s1.objects.Objects;
import org.s1.table.errors.AlreadyExistsException;
import org.s1.table.errors.MoreThanOneFoundException;
import org.s1.table.errors.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * MongoDB Query helper
 * DEBUG - results
 */
public class MongoDBQueryHelper {

    private static final Logger LOG = LoggerFactory.getLogger(MongoDBQueryHelper.class);

    /**
     *
     * @param id
     * @return
     * @throws NotFoundException
     * @throws MoreThanOneFoundException
     */
    public static Map<String, Object> get(Id id)
            throws NotFoundException, MoreThanOneFoundException {
        return get(id, Objects.newHashMap(String.class,Object.class,
                "id",id.getEntity()
                ));
    }

    /**
     * Select exactly one record from collection
     *
     * @param c
     * @param search
     * @throws NotFoundException
     * @throws MoreThanOneFoundException
     * @return
     */
    public static Map<String, Object> get(CollectionId c, Map<String,Object> search)
    throws NotFoundException, MoreThanOneFoundException {
        ensureOnlyOne(c, search);

        DBCollection coll = MongoDBConnectionHelper.getConnection(c.getDatabase()).getCollection(c.getCollection());
        Map<String,Object> m = MongoDBFormat.toMap(coll.findOne(MongoDBFormat.fromMap(search)));
        if(LOG.isDebugEnabled())
            LOG.debug("MongoDB get result ("+c+", search:"+search+"\n\t> "+m);
        return m;
    }

    public static final String SCORE = "_score";

    /*public static long fullTextSearch(List<Map<String, Object>> res, CollectionId c,
                                      String fullTextQuery,
                                      Map<String,Object> search,
                                      Map<String,Object> fields, int skip, int max) {
        DB db = MongoDBConnectionHelper.getConnection(c.getDatabase());
        BasicDBObject request = new BasicDBObject();
        request.put("text", c.getCollection());
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
            DBObject o = db.getCollection(c.getCollection()).findOne(new BasicDBObject("_id",Objects.get(m,"obj._id")),
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
            LOG.debug("MongoDB list result ("+c+", search:"+search+", full-text:"+fullTextQuery+", fields:"+fields+", max:"+max+", skip:"+skip +
                    "\n\t>command result: "+cr +
                    "\n\t>count: "+cnt +
                    "\n\t> "+res);

        return cnt;
    }*/

    /**
     *
     * @param c
     * @param search
     * @param sort
     * @param fields
     * @param skip
     * @param max
     * @return
     */
    public static List<Map<String, Object>> list(CollectionId c,
                            Map<String,Object> search, Map<String,Object> sort,
                            Map<String,Object> fields, int skip, int max) {
        List<Map<String, Object>> res = Objects.newArrayList();
        DBCollection coll = MongoDBConnectionHelper.getCollection(c);

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

        while (cur.hasNext()) {
            DBObject obj = cur.next();

            Map<String,Object> m = MongoDBFormat.toMap(obj);

            res.add(m);
        }

        if(LOG.isDebugEnabled())
            LOG.debug("MongoDB list result ("+c+", search:"+search+", sort:"+sort+", fields:"+fields+", max:"+max+", skip:"+skip +
                    "\n\t> "+res);

        return res;
    }

    /**
     *
     * @param c
     * @param search
     * @throws AlreadyExistsException
     */
    public static void ensureNotExists(CollectionId c, Map<String,Object> search) throws AlreadyExistsException {
        DBCollection coll = MongoDBConnectionHelper.getConnection(c.getDatabase()).getCollection(c.getCollection());
        if(search==null)
            search = Objects.newHashMap();
        long cnt = coll.count(MongoDBFormat.fromMap(search));
        if(cnt>0){
            if(LOG.isDebugEnabled())
                LOG.debug("Record already exists: "+c+", search: "+search);
            throw new AlreadyExistsException("MongoDB "+c+", search: "+search);
        }
    }

    /**
     *
     * @param c
     * @param search
     * @throws NotFoundException
     * @throws MoreThanOneFoundException
     */
    public static void ensureOnlyOne(CollectionId c, Map<String,Object> search)
            throws NotFoundException, MoreThanOneFoundException{
        DBCollection coll = MongoDBConnectionHelper.getConnection(c.getDatabase()).getCollection(c.getCollection());
        if(search==null)
            search = Objects.newHashMap();
        long cnt = coll.count(MongoDBFormat.fromMap(search));
        if(cnt==0){
            if(LOG.isDebugEnabled())
                LOG.debug("Record not found in "+c+", search: "+search);
            throw new NotFoundException("MongoDB "+c+", search: "+search);
        }
        if(cnt>1){
            if(LOG.isDebugEnabled())
                LOG.debug("More than one record found ("+cnt+") found "+c+", search: "+search);
            throw new MoreThanOneFoundException("MongoDB "+c+", search: "+search);
        }
    }

}
