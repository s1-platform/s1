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
            String id = Objects.get(params,"id");
            Map<String,Object> data = Objects.get(params,"data");
            if(id == null || data == null || MongoDBFormat.parseId(id)==null){
                return;
            }
            Map<String,Object> search = Objects.newHashMap("id",id);
            data.put("id",id);
            int n = coll.update(
                    MongoDBFormat.fromMap(search),
                    MongoDBFormat.fromMap(data),command.equals("add"),false,WriteConcern.FSYNC_SAFE).getN();
            if(LOG.isDebugEnabled())
                LOG.debug("MongoDB records("+n+") "+(command.equals("add")?"added":"updated")+", id:"+id+", data:"+data);
        }else if("remove".equals(command)){
            String id = Objects.get(params,"id");
            if(id == null || MongoDBFormat.parseId(id)==null){
                return;
            }
            Map<String,Object> search = Objects.newHashMap("id",id);
            int n = coll.remove(MongoDBFormat.fromMap(search),WriteConcern.FSYNC_SAFE).getN();
            if(LOG.isDebugEnabled())
                LOG.debug("MongoDB records("+n+") removed, id:"+id);
        }
    }

    /**
     * Add record to collection
     *
     * @param instance
     * @param collection
     * @param id
     * @param data
     */
    public static void add(String instance, String collection, String id, Map<String, Object> data){

        ClusterNode.call(MongoDBDDS.class, "add", Objects.newHashMap(String.class,Object.class,
                "instance",instance,
                "collection",collection,
                "id",id,
                "data",data
                ),getGroup(instance,collection,id));

    }

    /**
     * Update record
     *
     * @param instance
     * @param collection
     * @param id
     * @param data
     */
    public static void set(String instance, String collection, String id, Map<String, Object> data){

        ClusterNode.call(MongoDBDDS.class, "set", Objects.newHashMap(String.class,Object.class,
                "instance",instance,
                "collection",collection,
                "id",id,
                "data",data
        ),getGroup(instance,collection,id));

    }

    /**
     * Delete record
     *
     * @param instance
     * @param collection
     * @param id
     */
    public static void remove(String instance, String collection, String id){
        ClusterNode.call(MongoDBDDS.class, "remove", Objects.newHashMap(String.class,Object.class,
                "instance",instance,
                "collection",collection,
                "id",id
        ),getGroup(instance,collection,id));
    }

    /**
     *
     * @param instance
     * @param collection
     * @param id
     */
    public static void waitForRecord(String instance, String collection, String id){
        ClusterNode.flush(MongoDBDDS.class, getGroup(instance, collection, id));
    }

    /**
     *
     * @param instance
     * @param collection
     * @param id
     * @return
     */
    public static String getGroup(String instance, String collection,  String id){
        return instance+":"+collection+":"+id;
    }

}
