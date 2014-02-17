package org.s1.mongodb.log;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;
import org.s1.S1SystemError;
import org.s1.log.Loggers;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.objects.Objects;
import org.s1.user.AuthException;
import org.slf4j.Logger;

import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * MongoDB Log4j appender implementation
 */
public class MongoDBLog4jAppender extends AppenderSkeleton{

    private volatile DBCollection collection;

    public MongoDBLog4jAppender(){
        collection = MongoDBConnectionHelper.getConnection(MongoDBLogStorage.DB_INSTANCE)
                .getCollection(MongoDBLogStorage.COLLECTION);
    }

    @Override
    protected void append(LoggingEvent loggingEvent) {
        try{
            Map<String,Object> m = Loggers.toMap(loggingEvent);
            collection.insert(new BasicDBObject(m));
        }catch (Throwable e){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            System.err.println(sdf.format(new Date())+": MongoDBLog4jAppender error");
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}
