package org.s1.mongodb.log;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.s1.log.LogStorage;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.mongodb.MongoDBFormat;
import org.s1.mongodb.MongoDBQueryHelper;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.s1.table.format.Query;
import org.s1.table.format.Sort;

import java.util.List;
import java.util.Map;

/**
 * MongoDB Log storage implementation
 */
public class MongoDBLogStorage extends LogStorage {

    public static final String DB_INSTANCE = "log4j";
    public static final String COLLECTION = "log4j";

    public MongoDBLogStorage() {
        DBCollection coll = MongoDBConnectionHelper.getConnection(MongoDBLogStorage.DB_INSTANCE)
                .getCollection(COLLECTION);
        coll.ensureIndex(new BasicDBObject("date",1));
        coll.ensureIndex(new BasicDBObject("level",1));
        coll.ensureIndex(new BasicDBObject("fileName",1));
        coll.ensureIndex(new BasicDBObject("id",1));
        coll.ensureIndex(new BasicDBObject("sessionId",1));
        coll.ensureIndex(new BasicDBObject("name",1));
    }

    @Override
    public long list(List<Map<String, Object>> list, Query search, int skip, int max) {
        return MongoDBQueryHelper.list(list,DB_INSTANCE,COLLECTION,
                MongoDBFormat.formatSearch(search),
                MongoDBFormat.formatSort(new Sort("date",true)), null, skip, max);
    }
}