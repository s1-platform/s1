package org.s1.mongodb.table;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.s1.cluster.datasource.MoreThanOneFoundException;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.mongodb.MongoDBAggregationHelper;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.mongodb.MongoDBDDS;
import org.s1.mongodb.MongoDBQueryHelper;
import org.s1.objects.Objects;
import org.s1.table.AggregationBean;
import org.s1.table.CountGroupBean;
import org.s1.table.IndexBean;
import org.s1.table.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * MongoDB Table implementation
 */
public class MongoDBTable extends Table{

    private static final Logger LOG = LoggerFactory.getLogger(MongoDBTable.class);

    private List<String> fullTextFields = Objects.newArrayList();
    private String fullTextLanguage;

    /**
     *
     * @return
     */
    public List<String> getFullTextFields() {
        return fullTextFields;
    }

    /**
     *
     * @return
     */
    public String getFullTextLanguage() {
        return fullTextLanguage;
    }

    /**
     *
     * @param fullTextLanguage
     */
    public void setFullTextLanguage(String fullTextLanguage) {
        this.fullTextLanguage = fullTextLanguage;
    }

    @Override
    public void init() {
        super.init();
        DBCollection coll = MongoDBConnectionHelper.getConnection(null).getCollection(getCollection());
        List<DBObject> ftlist = coll.getIndexInfo();

        boolean exists = false;

        for(DBObject o: ftlist){
            if("full_text_index".equals(o.get("name"))){
                exists = true;
                List<String> keys = Objects.newArrayList();
                Map<String,Object> ks = (Map<String,Object>)o.get("weights");
                for(String k:ks.keySet()){
                    keys.add(k);
                }
                boolean rm = false;
                if(keys.size() == fullTextFields.size()){
                    for(String k:keys){
                        if(!fullTextFields.contains(k)){
                            rm = true;
                            break;
                        }
                    }
                }else{
                    rm = true;
                }
                if(rm){
                    //drop index
                    if(LOG.isDebugEnabled())
                        LOG.debug("Drop full_text_index for collection "+getCollection());
                    coll.dropIndex("full_text_index");
                    exists = false;
                }
            }
        }

        if(!exists){
            //create full-text indexes
            BasicDBObject o = new BasicDBObject();
            BasicDBObject opt = new BasicDBObject("name","full_text_index");

            //add index
            for(String s:fullTextFields){
                o.put(s,"text");
            }
            if(!Objects.isNullOrEmpty(fullTextLanguage)){
                opt.put("default_language",fullTextLanguage);
            }
            if(LOG.isDebugEnabled())
                LOG.debug("Ensure full_text_index for collection "+getCollection()+", fields: "+fullTextFields+", language: "+fullTextLanguage);
            coll.ensureIndex(o,opt);
        }
    }

    @Override
    public void fromMap(Map<String, Object> m) {
        super.fromMap(m);
        fullTextLanguage = Objects.get(m,"fullText.language");
        List<String> l = Objects.get(m,"fullText.fields");
        fullTextFields.clear();
        if(l!=null){
            fullTextFields.addAll(l);
        }
    }

    @Override
    protected void collectionIndex(String collection, String name, IndexBean ind) {
        DBObject i = new BasicDBObject();
        for(String f:ind.getFields()){
            i.put(f,1);
        }
        MongoDBConnectionHelper.getConnection(null).getCollection(collection).ensureIndex(i,name);
    }

    @Override
    protected void enrich(Map<String, Object> record, boolean list, Map<String, Object> ctx) {
        super.enrich(record, list, ctx);
        record.remove("_id");
    }

    @Override
    protected long collectionList(String collection, List<Map<String, Object>> result, String fullTextQuery, Map<String, Object> search, Map<String, Object> sort, Map<String, Object> fields, int skip, int max) {
        if(Objects.isNullOrEmpty(fullTextQuery)){
            return MongoDBQueryHelper.list(result,null,collection,search,sort,fields,skip,max);
        }else{
            return MongoDBQueryHelper.list(result,null,collection,fullTextQuery,search,fields,skip,max);
        }
    }

    @Override
    protected Map<String, Object> setFieldEqualsSearch(Map<String,Object> search, String name, String value) {
        if(search==null)
            search = Objects.newHashMap();
        search.put(name,value);
        return search;
    }

    @Override
    protected Map<String, Object> collectionGet(String collection, Map<String, Object> search) throws NotFoundException, MoreThanOneFoundException {
        return MongoDBQueryHelper.get(null,collection,search);
    }

    @Override
    protected AggregationBean collectionAggregate(String collection, String field, Map<String, Object> search) {
        return MongoDBAggregationHelper.aggregate(null,collection,field,search);
    }

    @Override
    protected List<CountGroupBean> collectionCountGroup(String collection, String field, Map<String, Object> search) {
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
