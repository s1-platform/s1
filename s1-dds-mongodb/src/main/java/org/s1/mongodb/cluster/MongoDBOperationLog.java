package org.s1.mongodb.cluster;

import com.mongodb.*;
import org.s1.cluster.node.MessageBean;
import org.s1.cluster.node.NodeOperationLog;
import org.s1.misc.Closure;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.mongodb.MongoDBFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * MongoDB cluster log implementation
 */
public class MongoDBOperationLog extends NodeOperationLog{

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
    public synchronized void listFrom(long id, Closure<MessageBean, Object> cl) {
        DBCollection coll = getCollection();
        DBCursor cur = coll.find(new BasicDBObject("id",new BasicDBObject("$gt",id)))
            .sort(new BasicDBObject("id", 1));
        while(cur.hasNext()){
            DBObject o = cur.next();
            MessageBean m = new MessageBean();
            m.fromMap(MongoDBFormat.toMap(o));
            cl.callQuite(m);
        }
    }

    @Override
    public synchronized void listUndone(Closure<MessageBean, Object> cl) {
        DBCollection coll = getCollection();
        DBCursor cur = coll.find(new BasicDBObject("done",false))
                .sort(new BasicDBObject("id", 1));
        while(cur.hasNext()){
            DBObject o = cur.next();
            MessageBean m = new MessageBean();
            m.fromMap(MongoDBFormat.toMap(o));
            cl.callQuite(m);
        }
    }

    @Override
    public synchronized void addToLocalLog(MessageBean m) {
        DBCollection coll = getCollection();
        Map<String,Object> m1 = m.toMap();
        m1.put("done",false);
        coll.insert(MongoDBFormat.fromMap(m1), WriteConcern.FSYNC_SAFE);
        if(LOG.isTraceEnabled()){
            LOG.trace("Node write log new record: "+m.toString(true));
        }else if(LOG.isDebugEnabled()){
            LOG.debug("Node write log new record: "+m.toString(false));
        }
    }

    @Override
    public synchronized void markDone(long id) {
        DBCollection coll = getCollection();
        coll.update(new BasicDBObject("id",id),
                new BasicDBObject("$set",new BasicDBObject("done",true)),false,false, WriteConcern.FSYNC_SAFE);
        if(LOG.isDebugEnabled())
            LOG.debug("Node write log record #"+id+" marked as done:true");
    }

    @Override
    public synchronized long getLastId() {
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

    private DBCollection getCollection(){
        DBCollection coll = MongoDBConnectionHelper.getConnection("clusterLog").getCollection("clusterLog");
        return coll;
    }
}
