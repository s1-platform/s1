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
        coll.ensureIndex(new BasicDBObject("messageId",1));
        coll.ensureIndex(new BasicDBObject("done",1));
        LOG.info("Operation write log initialized, indexes checked");
    }

    @Override
    public void listFrom(long id, Closure<MessageBean, Object> cl) {
        DBCollection coll = getCollection();
        DBCursor cur = coll.find(new BasicDBObject("messageId",new BasicDBObject("$gt",id)))
            .sort(new BasicDBObject("messageId", 1));
        while(cur.hasNext()){
            DBObject o = cur.next();
            MessageBean m = new MessageBean();
            Map<String,Object> m1 = MongoDBFormat.toMap(o);
            m1.put("id",m1.get("messageId"));
            m1.remove("messageId");
            m.fromMap(m1);
            cl.callQuite(m);
        }
    }

    @Override
    public void listUndone(Closure<MessageBean, Object> cl) {
        DBCollection coll = getCollection();
        DBCursor cur = coll.find(new BasicDBObject("done",false))
                .sort(new BasicDBObject("messageId", 1));
        while(cur.hasNext()){
            DBObject o = cur.next();
            MessageBean m = new MessageBean();
            Map<String,Object> m1 = MongoDBFormat.toMap(o);
            m1.put("id",m1.get("messageId"));
            m1.remove("messageId");
            m.fromMap(m1);
            cl.callQuite(m);
        }
    }

    @Override
    public void addToLocalLog(MessageBean m) {
        DBCollection coll = getCollection();
        Map<String,Object> m1 = m.toMap();
        m1.put("done",false);
        m1.put("messageId",m1.get("id"));
        m1.remove("id");
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
        coll.update(new BasicDBObject("messageId",id),
                new BasicDBObject("$set",new BasicDBObject("done",true)),false,false, WriteConcern.FSYNC_SAFE);
        if(LOG.isDebugEnabled())
            LOG.debug("Node write log record #"+id+" marked as done:true");
    }

    @Override
    public long getLastId() {
        DBCollection coll = getCollection();
        DBCursor cur = coll.find().sort(new BasicDBObject("messageId", -1)).limit(1);
        DBObject o = null;
        if(cur.hasNext())
            o = cur.next();
        if(o!=null){
            return (Long)o.get("messageId");
        }
        return 0;
    }

    public static final String DB_INSTANCE = "clusterLog";
    public static final String COLLECTION = "clusterLog";

    private DBCollection getCollection(){
        DBCollection coll = MongoDBConnectionHelper.getConnection(DB_INSTANCE).getCollection(COLLECTION);
        return coll;
    }
}
