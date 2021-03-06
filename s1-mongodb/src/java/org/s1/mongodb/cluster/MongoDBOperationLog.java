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

package org.s1.mongodb.cluster;

import com.mongodb.*;
import org.s1.S1SystemError;
import org.s1.cluster.dds.DistributedDataSource;
import org.s1.cluster.dds.OperationLog;
import org.s1.cluster.dds.beans.MessageBean;
import org.s1.misc.Closure;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.mongodb.MongoDBFormat;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * MongoDB cluster log implementation
 */
public class MongoDBOperationLog extends OperationLog {

    private static final Logger LOG = LoggerFactory.getLogger(MongoDBOperationLog.class);

    @Override
    public void initialize() {
        super.initialize();
        DBCollection coll = getCollection();
        coll.ensureIndex(new BasicDBObject("id",1));
        coll.ensureIndex(new BasicDBObject("done",1));
        LOG.info("Operation write log initialized, indexes checked");
    }

    @Override
    public void listFrom(long id, Closure<MessageBean, Object> cl) {
        DBCollection coll = getCollection();
        DBCursor cur = coll.find(new BasicDBObject("id",new BasicDBObject("$gt",id)))
            .sort(new BasicDBObject("id", 1));
        while(cur.hasNext()){
            DBObject o = cur.next();
            Map<String,Object> m1 = MongoDBFormat.toMap(o);
            MessageBean mb = new MessageBean();
            mb.fromMap(m1);
            cl.call(mb);
        }
    }

    @Override
    public void listUndone(Closure<MessageBean, Object> cl) {
        DBCollection coll = getCollection();
        DBCursor cur = coll.find(new BasicDBObject("done",false))
                .sort(new BasicDBObject("id", 1));
        while(cur.hasNext()){
            DBObject o = cur.next();
            Map<String,Object> m1 = MongoDBFormat.toMap(o);
            MessageBean mb = new MessageBean();
            mb.fromMap(m1);
            cl.call(mb);
        }
    }

    @Override
    public void addToLocalLog(MessageBean m) {
        DBCollection coll = getCollection();
        Map<String,Object> m1 = m.toMap();
        m1.put("done", false);
        coll.insert(MongoDBFormat.fromMap(m1), WriteConcern.FSYNC_SAFE);
        if(LOG.isTraceEnabled()){
            LOG.trace("Node write log new record: "+m.toString(true));
        }else if(LOG.isDebugEnabled()){
            LOG.debug("Node write log new record: "+m.toString(false));
        }
    }

    @Override
    public void markDone(long id) {
        DBCollection coll = getCollection();
        coll.update(new BasicDBObject("id",id),
                new BasicDBObject("$set",new BasicDBObject("done",true)),false,false, WriteConcern.FSYNC_SAFE);
        if(LOG.isDebugEnabled())
            LOG.debug("Node write log record #"+id+" marked as done:true");
    }

    @Override
    public long getLastId() {
        DBCollection coll = getCollection();
        DBCursor cur = coll.find().sort(new BasicDBObject("id", -1)).limit(1);
        DBObject o = null;
        if(cur.hasNext())
            o = cur.next();
        if(o!=null){
            return (Long)o.get("id");
        }
        return 0;
    }

    private static DBCollection coll;

    private static DBCollection getCollection(){
        if(coll==null) {
            synchronized (MongoDBOperationLog.class){
                if(coll==null){
                    String c = Options.getStorage().get("MongoDB", "connections.clusterLog.collection", "clusterLog");
                    coll = MongoDBConnectionHelper.getConnection("clusterLog").getCollection(c);
                }
            }
        }
        return coll;
    }

}
