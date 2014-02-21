package org.s1.mongodb.table;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.s1.cluster.datasource.MoreThanOneFoundException;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.mongodb.MongoDBAggregationHelper;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.mongodb.MongoDBDDS;
import org.s1.mongodb.MongoDBQueryHelper;
import org.s1.objects.Objects;
import org.s1.table.IndexBean;
import org.s1.table.Table;

import java.util.List;
import java.util.Map;

/**
 * MongoDB Table implementation
 */
public class MongoDBTable extends Table{

    @Override
    protected void collectionIndex(String collection, String name, IndexBean ind) {
        DBObject i = new BasicDBObject();
        for(String f:ind.getFields()){
            i.put(f,1);
        }
        MongoDBConnectionHelper.getConnection(null).getCollection(collection).ensureIndex(i,name);
    }

    @Override
    protected long collectionList(String collection, List<Map<String, Object>> result, Map<String, Object> search, Map<String, Object> sort, Map<String, Object> fields, int skip, int max) {
        return MongoDBQueryHelper.list(result,null,collection,search,sort,fields,skip,max);
    }

    @Override
    protected Map<String, Object> getFieldEqualsSearch(String name, String value) {
        return Objects.newHashMap(String.class,Object.class,name,value);
    }

    @Override
    protected Map<String, Object> collectionGet(String collection, Map<String, Object> search) throws NotFoundException, MoreThanOneFoundException {
        return MongoDBQueryHelper.get(null,collection,search);
    }

    @Override
    protected Map<String, Object> collectionAggregate(String collection, String field, Map<String, Object> search) {
        return MongoDBAggregationHelper.aggregate(null,collection,field,search);
    }

    @Override
    protected List<Map<String, Object>> collectionCountGroup(String collection, String field, Map<String, Object> search) {
        return MongoDBAggregationHelper.countGroup(null, collection, field, search);
    }

    @Override
    protected void collectionAdd(String collection, Map<String, Object> data) {
        MongoDBDDS.add(null,collection,Objects.newHashMap(String.class,Object.class,"id",Objects.get(data,"id")),data);
    }

    @Override
    protected void collectionSet(String collection, String id, Map<String, Object> data) {
        MongoDBDDS.set(null, collection, Objects.newHashMap(String.class, Object.class, "id", id), data);
    }

    @Override
    protected void collectionRemove(String collection, String id) {
        MongoDBDDS.remove(null, collection, Objects.newHashMap(String.class, Object.class, "id", id));
    }

    @Override
    protected Map<String, Object> getUniqueSearch(boolean isNew, String id, Map<String, Object> pathsAndValues) {
        Map<String,Object> s = pathsAndValues;
        if(!isNew){
            s = Objects.newHashMap("$and",Objects.newArrayList(pathsAndValues, Objects.newHashMap(
                    "$ne", Objects.newHashMap("id",id)
           ))); 
        }
        return s;
    }
}
