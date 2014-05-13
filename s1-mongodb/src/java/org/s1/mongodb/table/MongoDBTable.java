package org.s1.mongodb.table;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.s1.cluster.dds.beans.CollectionId;
import org.s1.cluster.dds.beans.Id;
import org.s1.cluster.dds.beans.StorageId;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.mongodb.MongoDBFormat;
import org.s1.mongodb.MongoDBQueryHelper;
import org.s1.mongodb.cluster.MongoDBDDS;
import org.s1.objects.MapMethod;
import org.s1.objects.Objects;
import org.s1.table.Table;
import org.s1.table.errors.MoreThanOneFoundException;
import org.s1.table.errors.NotFoundException;
import org.s1.user.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Grigory Pykhov
 */
public abstract class MongoDBTable extends Table {

    private static final Logger LOG = LoggerFactory.getLogger(MongoDBTable.class);

    public abstract CollectionId getCollectionId();

    public void init() {
        //indexes
        checkIndexes();
    }
    public abstract List<List<String>> getIndexes();

    protected void checkIndexes() {
        //remove old indexes
        DBCollection coll = MongoDBConnectionHelper.getCollection(getCollectionId());
        List<DBObject> list = coll.getIndexInfo();
        List<List<String>> indexes = new ArrayList<List<String>>(getIndexes());
        indexes.add(Objects.newArrayList("id"));
        for(DBObject o: list){
            DBObject key = (DBObject)o.get("key");
            if(key.keySet().size()==1 && Objects.equals(key.get("_id"),1)){
                continue;
            }

            boolean exists = false;
            for (List<String> b : indexes) {
                boolean same = false;
                if(key.keySet().size()==b.size()){
                    same = true;
                    for(String s:b){
                        if(!Objects.equals(1,key.get(s))){
                            same = false;
                        }
                    }
                }
                exists=same;
                if(exists)
                    break;
            }
            if(exists)
                continue;

            //removing
            if(LOG.isDebugEnabled())
                LOG.debug("Drop index "+o+" for collection "+getCollectionId().getCollection());
            coll.dropIndex((String)o.get("name"));
        }

        //add new
        for(List<String> b:indexes){
            boolean exists = false;
            for(DBObject o: list){
                DBObject key = (DBObject)o.get("key");
                if(key.keySet().size()==1 && Objects.equals(1, key.get("id"))){
                    boolean same = false;
                    if(key.keySet().size()==b.size()){
                        same = true;
                        for(String s:b){
                            if(!Objects.equals(1,key.get(s))){
                                same = false;
                            }
                        }
                    }
                    exists=same;
                    if(exists)
                        break;
                }
            }
            if(exists)
                break;

            //add new index
            if(LOG.isDebugEnabled())
                LOG.debug("Ensure index "+b+" for collection "+getCollectionId().getCollection());
            DBObject i = new BasicDBObject();
            for(String f:b){
                i.put(f,1);
            }
            coll.ensureIndex(i);
        }

    }

    protected void prepareSearch(Map<String,Object> search){

    }

    protected void prepareSort(Map<String,Object> sort){

    }

    @MapMethod(names = {"search"})
    public long count(Map<String,Object> search) throws AccessDeniedException {
        checkAccess();
        if(search==null)
            search = Objects.newSOHashMap();
        prepareSearch(search);
        return MongoDBConnectionHelper.getCollection(getCollectionId()).count(MongoDBFormat.fromMap(search));
    }

    public List<Map<String,Object>> list(Map<String,Object> search, Map<String,Object> sort, Map<String,Object> fields, int skip, int max) throws AccessDeniedException {
        return list(search,sort,fields,skip,max,null);
    }

    @MapMethod(names = {"search","sort","fields","skip","max","ctx"})
    public List<Map<String,Object>> list(Map<String,Object> search, Map<String,Object> sort, Map<String,Object> fields, int skip, int max, Map<String,Object> ctx) throws AccessDeniedException {
        checkAccess();
        if(search==null)
            search = Objects.newSOHashMap();
        search = MongoDBFormat.escapeInjections(search);
        prepareSearch(search);
        if(sort==null)
            sort = Objects.newSOHashMap();
        prepareSort(sort);
        List<Map<String,Object>> l = MongoDBQueryHelper.list(getCollectionId(),search,sort,fields,skip,max);
        for(Map<String,Object> m:l){
            enrichRecord(m,ctx);
        }
        return l;
    }

    @Override
    public Map<String, Object> collectionGet(String id) throws NotFoundException, MoreThanOneFoundException {
        return MongoDBQueryHelper.get(getCollectionId(), Objects.newSOHashMap("id",id));
    }

    @Override
    public void collectionAdd(String id, Map<String, Object> data) {
        MongoDBDDS.add(new Id(getCollectionId().getDatabase(), getCollectionId().getCollection(), id), data);
    }

    @Override
    public void collectionSet(String id, Map<String, Object> data) {
        MongoDBDDS.set(new Id(getCollectionId().getDatabase(), getCollectionId().getCollection(), id), data);
    }

    @Override
    public void collectionRemove(String id) {
        MongoDBDDS.remove(new Id(getCollectionId().getDatabase(), getCollectionId().getCollection(), id));
    }

    @Override
    public String getName() {
        return new StorageId(MongoDBDDS.class,getCollectionId().getDatabase(),getCollectionId().getCollection(),null).getLockName();
    }
}
