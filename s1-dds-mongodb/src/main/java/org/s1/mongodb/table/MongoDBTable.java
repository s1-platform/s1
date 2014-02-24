package org.s1.mongodb.table;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.s1.cluster.datasource.MoreThanOneFoundException;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.mongodb.*;
import org.s1.objects.Objects;
import org.s1.table.AggregationBean;
import org.s1.table.CountGroupBean;
import org.s1.table.IndexBean;
import org.s1.table.Table;
import org.s1.table.format.FieldsMask;
import org.s1.table.format.Query;
import org.s1.table.format.QueryNode;
import org.s1.table.format.Sort;
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
    protected long collectionList(String collection, List<Map<String, Object>> result, String fullTextQuery,
                                  Query search, Sort sort, FieldsMask fields, int skip, int max) {
        if(Objects.isNullOrEmpty(fullTextQuery)){
            return MongoDBQueryHelper.list(result,null,collection,
                    MongoDBFormat.formatSearch(search),
                    MongoDBFormat.formatSort(sort),
                    MongoDBFormat.formatFieldsMask(fields),skip,max);
        }else{
            return MongoDBQueryHelper.fullTextSearch(result, null, collection, fullTextQuery,
                    MongoDBFormat.formatSearch(search),
                    MongoDBFormat.formatFieldsMask(fields), skip, max);
        }
    }

    @Override
    protected Map<String, Object> collectionGet(String collection, Query search) throws NotFoundException, MoreThanOneFoundException {
        return MongoDBQueryHelper.get(null,collection,MongoDBFormat.formatSearch(search));
    }

    @Override
    protected AggregationBean collectionAggregate(String collection, String field, Query search) {
        return MongoDBAggregationHelper.aggregate(null,collection,field,MongoDBFormat.formatSearch(search));
    }

    @Override
    protected List<CountGroupBean> collectionCountGroup(String collection, String field, Query search) {
        return MongoDBAggregationHelper.countGroup(null, collection, field, MongoDBFormat.formatSearch(search));
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

}
