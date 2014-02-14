package org.s1.mongodb;

import com.mongodb.*;
import org.s1.cluster.datasource.DistributedDataSource;
import org.s1.cluster.node.ClusterNode;
import org.s1.objects.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * MongoDB CRUD helper
 * Provides CRUD operations with locks
 */
public class MongoDBDDS extends DistributedDataSource{

    private static final Logger LOG = LoggerFactory.getLogger(MongoDBDDS.class);

    @Override
    public void runWriteCommand(String command, Map<String, Object> params) {
        String cn = Objects.get(params,"collection");
        if(Objects.isNullOrEmpty(cn)){
            return;
        }
        DBCollection coll = MongoDBConnectionHelper.getConnection(Objects.get(params,"instance",""))
                .getCollection(cn);

        if("add".equals(command) || "set".equals(command)){
            Map<String,Object> search = Objects.get(params,"search");
            Map<String,Object> data = Objects.get(params,"data");
            if(search == null || data == null){
                return;
            }
            int n = coll.update(
                    MongoDBFormat.fromMap(search),
                    MongoDBFormat.fromMap(data),command.equals("add"),false,WriteConcern.FSYNC_SAFE).getN();
            if(LOG.isDebugEnabled())
                LOG.debug("MongoDB records("+n+") "+(command.equals("add")?"added":"updated")+", search:"+search+", data:"+data);
        }else if("remove".equals(command)){
            Map<String,Object> search = Objects.get(params,"search");
            if(search == null){
                return;
            }
            int n = coll.remove(MongoDBFormat.fromMap(search),WriteConcern.FSYNC_SAFE).getN();
            if(LOG.isDebugEnabled())
                LOG.debug("MongoDB records("+n+") removed, search:"+search);
        }
    }

    /**
     * Add record to collection
     *
     * @param instance
     * @param collection
     * @param search
     * @param data
     */
    public static void add(String instance, String collection, Map<String, Object> search, Map<String, Object> data){

        ClusterNode.call(MongoDBDDS.class, "add", Objects.newHashMap(String.class,Object.class,
                "instance",instance,
                "collection",collection,
                "search",search,
                "data",data
                ),getGroup(instance,collection,search));

    }

    /**
     * Update record
     *
     * @param instance
     * @param collection
     * @param search
     * @param data
     */
    public static void set(String instance, String collection, Map<String, Object> search, Map<String, Object> data){

        ClusterNode.call(MongoDBDDS.class, "set", Objects.newHashMap(String.class,Object.class,
                "instance",instance,
                "collection",collection,
                "search",search,
                "data",data
        ),getGroup(instance,collection,search));

    }

    /**
     * Delete record
     *
     * @param instance
     * @param collection
     * @param search
     */
    public static void remove(String instance, String collection, Map<String, Object> search){
        ClusterNode.call(MongoDBDDS.class, "remove", Objects.newHashMap(String.class,Object.class,
                "instance",instance,
                "collection",collection,
                "search",search
        ),getGroup(instance,collection,search));
    }

    /**
     *
     * @param collection
     * @param search
     */
    public static void waitForRecord(String instance, String collection, Map<String, Object> search){
        ClusterNode.flush(MongoDBDDS.class,getGroup(instance,collection,search));
    }

    /**
     *
     * @param instance
     * @param collection
     * @param search
     * @return
     */
    private static String getGroup(String instance, String collection,  Map<String, Object> search){
        return instance+":"+collection+":"+search;
    }

}
