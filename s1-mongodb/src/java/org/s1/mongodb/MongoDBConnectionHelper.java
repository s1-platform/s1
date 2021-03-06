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

import com.mongodb.*;
import org.s1.S1SystemError;
import org.s1.cluster.dds.beans.CollectionId;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MongoDB connection helper
 */
public class MongoDBConnectionHelper {

    private static final Logger LOG = LoggerFactory.getLogger(MongoDBConnectionHelper.class);

    /**
     *
     */
    private static Map<String,DB> connections = new ConcurrentHashMap<String, DB>();

    public static final String OPTIONS = "MongoDB";

    /**
     *
     * @param instance
     */
    private static synchronized void initialize(String instance) {
        if(!connections.containsKey(instance)){

            Map<String,Object> mopt = Options.getStorage().getMap(OPTIONS);
            Map<String,Object> m = Objects.get(mopt,"connections."+instance);
            if(Objects.isNullOrEmpty(m)){
                m = Objects.get(mopt,"connections."+DEFAULT_INSTANCE);
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
            LOG.info("MongoDB connected " + cl.getAddress().getHost() + ":" + cl.getAddress().getPort());

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

    public static DBCollection getCollection(CollectionId id) {
        return getConnection(id.getDatabase()).getCollection(id.getCollection());
    }
}
