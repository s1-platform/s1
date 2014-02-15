package org.s1.mongodb.log;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.s1.S1SystemError;
import org.s1.log.Loggers;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.objects.Objects;
import org.s1.user.AuthException;

import java.net.UnknownHostException;
import java.util.Map;

/**
 * MongoDB Log4j appender implementation
 */
public class MongoDBLog4jAppender extends AppenderSkeleton{

    private String host;
    private String db;
    private String collection;
    private int port;
    private String user;
    private String password;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHost() {
        if(host==null)
            host = "localhost";
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getDb() {
        if(db==null)
            db = "log4j";
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public String getCollection() {
        if(collection==null)
            collection = "log4j";
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public int getPort() {
        if(port==0)
            port = 27017;
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    private volatile DB local;

    protected DB getLocalDB(){
        if(local==null){
            synchronized (this){
                if(local==null){
                    MongoClient cl = null;
                    try {
                        cl = new MongoClient(getHost(),getPort());
                    } catch (UnknownHostException e) {
                        throw S1SystemError.wrap(e);
                    }
                    local = cl.getDB(getDb());
                    if(!Objects.isNullOrEmpty(getUser())){
                        if(!local.authenticate(getUser(),getPassword().toCharArray()))
                            throw new S1SystemError("Cannot authenticate to mongodb "+getDb()+", user "+getUser());
                    }
                }
            }
        }
        return local;
    }

    @Override
    protected void append(LoggingEvent loggingEvent) {
        Map<String,Object> m = Loggers.toMap(loggingEvent);
        DBCollection coll = getLocalDB().getCollection(getCollection());
        coll.insert(new BasicDBObject(m));
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}
