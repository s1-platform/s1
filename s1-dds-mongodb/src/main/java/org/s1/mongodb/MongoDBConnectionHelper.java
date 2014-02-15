package org.s1.mongodb;

import com.mongodb.*;
import org.s1.S1SystemError;
import org.s1.objects.Objects;
import org.s1.options.Options;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MongoDB connection helper
 */
public class MongoDBConnectionHelper {

    /**
     *
     */
    private static Map<String,DB> connections = new ConcurrentHashMap<String, DB>();

    /**
     *
     * @param instance
     */
    private static synchronized void initialize(String instance) {
        if(!connections.containsKey(instance)){

            Map<String,Object> mopt = Options.getStorage().getMap("MongoDB");
            Map<String,Object> m = Objects.get(mopt,instance);
            if(Objects.isNullOrEmpty(m)){
                m = Objects.get(mopt,DEFAULT_INSTANCE);
            }

            MongoClientOptions.Builder b = MongoClientOptions.builder();
            MongoClientOptions def_opt = MongoClientOptions.builder().build();
            b.connectionsPerHost(Objects.get(m,"connectionsPerHost",def_opt.getConnectionsPerHost()));
            b.autoConnectRetry(Objects.get(m,"autoConnectRetry",def_opt.isAutoConnectRetry()));
            b.connectTimeout(Objects.get(m,"connectTimeout",def_opt.getConnectTimeout()));
            b.socketKeepAlive(Objects.get(m,"socketKeepAlive",def_opt.isSocketKeepAlive()));
            b.socketTimeout(Objects.get(m,"socketTimeout",def_opt.getSocketTimeout()));
            b.maxAutoConnectRetryTime(Objects.get(m,"maxAutoConnectRetryTime",def_opt.getMaxAutoConnectRetryTime()));
            b.maxWaitTime(Objects.get(m,"maxWaitTime",def_opt.getMaxWaitTime()));
            b.threadsAllowedToBlockForConnectionMultiplier(Objects.get(m, "threadsAllowedToBlockForConnectionMultiplier", def_opt.getThreadsAllowedToBlockForConnectionMultiplier()));
            b.writeConcern(WriteConcern.FSYNC_SAFE);
            MongoClientOptions opt = b.build();

            MongoClient cl = null;
            try {
                cl = new MongoClient(new ServerAddress(Objects.get(m, "host", "localhost"),Objects.get(m,"port",27017)),opt);
            } catch (UnknownHostException e) {
                throw S1SystemError.wrap(e);
            }

            String dbName = Objects.get(m,"name");
            if(Objects.isNullOrEmpty(dbName))
                throw new S1SystemError("Cannot initialize MongoDB connection, because name is not set");

            DB db = cl.getDB(dbName);

            String user = Objects.get(m,"user");
            String password = Objects.get(m,"password");
            if(!Objects.isNullOrEmpty(user)){
                if(!db.authenticate(user,password.toCharArray())){
                    throw new S1SystemError("Cannot authenticate MongoDB connection "+dbName+" with user "+user);
                }
            }
            System.out.println(Objects.formatDate(new Date(),"yyyy-MM-dd HH:mm:ss.SSS")+ " INFO MongoDBConnectionHelper: MongoDB connected "+cl.getAddress().getHost()+":"+cl.getAddress().getPort());
            connections.put(instance,db);
        }
    }

    public static final String DEFAULT_INSTANCE = "default";

    /**
     * Get connection
     *
     * @param instance
     * @return
     */
    public static DB getConnection(String instance) {
        if(Objects.isNullOrEmpty(instance))
            instance = DEFAULT_INSTANCE;
        if(!connections.containsKey(instance)){
            initialize(instance);
        }
        return connections.get(instance);
    }
}
