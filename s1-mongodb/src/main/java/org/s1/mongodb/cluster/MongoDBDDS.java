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

import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import org.s1.cluster.dds.DDSCluster;
import org.s1.cluster.dds.DistributedDataSource;
import org.s1.cluster.dds.beans.CommandBean;
import org.s1.cluster.dds.beans.Id;
import org.s1.cluster.dds.beans.MessageBean;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.mongodb.MongoDBFormat;
import org.s1.objects.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * MongoDB CRUD helper
 * Provides CRUD operations with locks
 */
public class MongoDBDDS extends DistributedDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(MongoDBDDS.class);

    @Override
    public void runWriteCommand(CommandBean b) {
        if(Objects.isNullOrEmpty(b.getCollection())){
            return;
        }
        if(Objects.isNullOrEmpty(b.getEntity())){
            return;
        }
        DBCollection coll = MongoDBConnectionHelper.getConnection(b.getDatabase())
                .getCollection(b.getCollection());

        if("add".equals(b.getCommand()) || "set".equals(b.getCommand())){
            if(b.getParams() == null){
                return;
            }
            Map<String,Object> search = Objects.newHashMap("id",b.getEntity());
            b.getParams().put("id",b.getEntity());
            int n = coll.update(
                    MongoDBFormat.fromMap(search),
                    MongoDBFormat.fromMap(b.getParams()),b.getCommand().equals("add"),false,WriteConcern.FSYNC_SAFE).getN();
            if(LOG.isDebugEnabled())
                LOG.debug("MongoDB records("+n+") "+
                        (b.getCommand().equals("add")?"added":"updated")+", "+b);
        }else if("remove".equals(b.getCommand())){
            Map<String,Object> search = Objects.newHashMap("id",b.getEntity());
            int n = coll.remove(MongoDBFormat.fromMap(search),WriteConcern.FSYNC_SAFE).getN();
            if(LOG.isDebugEnabled())
                LOG.debug("MongoDB records("+n+") removed, "+b);
        }
    }

    /**
     * Add record to collection
     *
     * @param id
     * @param data
     */
    public static void add(Id id, Map<String, Object> data){
        DDSCluster.call(new MessageBean(MongoDBDDS.class,
                id.getDatabase(),id.getCollection(),id.getEntity(),
                "add",data));
    }

    /**
     * Update record
     *
     * @param id
     * @param data
     */
    public static void set(Id id, Map<String, Object> data){
        DDSCluster.call(new MessageBean(MongoDBDDS.class,
                id.getDatabase(),id.getCollection(),id.getEntity(),
                "set",data));

    }

    /**
     * Delete record
     *
     * @param id
     */
    public static void remove(Id id){
        DDSCluster.call(new MessageBean(MongoDBDDS.class,
                id.getDatabase(),id.getCollection(),id.getEntity(),
                "remove",null));
    }

}
