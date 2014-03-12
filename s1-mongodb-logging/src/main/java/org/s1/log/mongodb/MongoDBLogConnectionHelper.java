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

package org.s1.log.mongodb;

import com.mongodb.*;
import org.s1.S1SystemError;
import org.s1.objects.Objects;
import org.s1.options.Options;

import java.net.UnknownHostException;
import java.util.Map;

/**
 * Connection helper
 */
public class MongoDBLogConnectionHelper {

    /**
     *
     */
    private static DBCollection local = null;

    /**
     *
     */
    private static synchronized void initialize() {

        Map<String,Object> m = Options.getStorage().getSystem("mongodbLog");

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
        b.writeConcern(WriteConcern.NONE);
        MongoClientOptions opt = b.build();

        MongoClient cl = null;
        try {
            cl = new MongoClient(new ServerAddress(Objects.get(m, "host", "localhost"),Objects.get(m,"port",27017)),opt);
        } catch (UnknownHostException e) {
            throw S1SystemError.wrap(e);
        }

        String dbName = Objects.get(m,"database","log4j");

        DB db = cl.getDB(dbName);

        String user = Objects.get(m,"user");
        String password = Objects.get(m,"password");
        if(!Objects.isNullOrEmpty(user)){
            if(!db.authenticate(user,password.toCharArray())){
                throw new S1SystemError("Cannot authenticate MongoDB connection "+dbName+" with user "+user);
            }
        }

        String collection = Objects.get(m,"collection","log4j");
        DBCollection coll = db.getCollection(collection);

        coll.ensureIndex(new BasicDBObject("date",1));
        coll.ensureIndex(new BasicDBObject("level",1));
        coll.ensureIndex(new BasicDBObject("fileName",1));
        coll.ensureIndex(new BasicDBObject("id",1));
        coll.ensureIndex(new BasicDBObject("sessionId",1));
        coll.ensureIndex(new BasicDBObject("name",1));

        local = coll;

    }

    /**
     * Get collection
     *
     * @return
     */
    public static DBCollection getCollection() {
        if(local==null){
            initialize();
        }
        return local;
    }

}
