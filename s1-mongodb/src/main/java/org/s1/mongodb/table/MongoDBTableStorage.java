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

package org.s1.mongodb.table;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.s1.cluster.dds.DistributedDataSource;
import org.s1.cluster.dds.beans.Id;
import org.s1.mongodb.MongoDBAggregationHelper;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.mongodb.MongoDBFormat;
import org.s1.mongodb.MongoDBQueryHelper;
import org.s1.mongodb.cluster.MongoDBDDS;
import org.s1.objects.Objects;
import org.s1.table.*;
import org.s1.table.errors.MoreThanOneFoundException;
import org.s1.table.errors.NotFoundException;
import org.s1.table.format.FieldsMask;
import org.s1.table.format.Query;
import org.s1.table.format.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * MongoDB Table storage implementation
 */
public class MongoDBTableStorage extends TableStorage{

    private static final Logger LOG = LoggerFactory.getLogger(MongoDBTableStorage.class);

    /**
     *
     * @return
     */
    public List<String> getFullTextFields() {
        return Objects.newArrayList();
    }

    /**
     *
     * @return
     */
    public String getFullTextLanguage() {
        return "english";
    }

    @Override
    public Class<? extends DistributedDataSource> getDataSource() {
        return MongoDBDDS.class;
    }

    @Override
    public void init() {
        //super.init();
        /*DBCollection coll = MongoDBConnectionHelper.getConnection(null).getCollection(getCollectionId().getCollection());
        List<DBObject> ftlist = coll.getIndexInfo();

        boolean exists = false;
        */
        /*for(DBObject o: ftlist){
            if("full_text_index".equals(o.get("name"))){
                exists = true;
                List<String> keys = Objects.newArrayList();
                Map<String,Object> ks = (Map<String,Object>)o.get("weights");
                for(String k:ks.keySet()){
                    keys.add(k);
                }
                boolean rm = false;
                if(keys.size() == getFullTextFields().size()){
                    for(String k:keys){
                        if(!getFullTextFields().contains(k)){
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
                        LOG.debug("Drop full_text_index for collection "+getCollectionId().getCollection());
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
            for(String s:getFullTextFields()){
                o.put(s,"text");
            }
            if(!Objects.isNullOrEmpty(getFullTextLanguage())){
                opt.put("default_language",getFullTextLanguage());
            }
            if(LOG.isDebugEnabled())
                LOG.debug("Ensure full_text_index for collection "+getCollectionId().getCollection()+
                        ", fields: "+getFullTextFields()+", language: "+getFullTextLanguage());
            if(!o.isEmpty())
                coll.ensureIndex(o,opt);
        }*/
    }

    @Override
    public void collectionIndex(String name, IndexBean ind) {
        DBObject i = new BasicDBObject();
        for(String f:ind.getFields()){
            i.put(f,1);
        }
        MongoDBConnectionHelper.getConnection(getTable().getCollectionId().getDatabase())
                .getCollection(getTable().getCollectionId().getCollection()).ensureIndex(i,name);
    }

    @Override
    public long collectionList(List<Map<String, Object>> result,
                                  Query search, Sort sort, FieldsMask fields, int skip, int max) {
        search.setCustom(MongoDBFormat.escapeInjections(search.getCustom()));
        /*String fullTextQuery = Objects.get(search.getCustom(),"$text");
        if(search.getCustom()!=null)
            search.getCustom().remove("$text");*/
        //if(Objects.isNullOrEmpty(fullTextQuery)){
            return MongoDBQueryHelper.list(result,getTable().getCollectionId(),
                    MongoDBFormat.formatSearch(search),
                    MongoDBFormat.formatSort(sort),
                    MongoDBFormat.formatFieldsMask(fields),skip,max);
        /*}else{
            return MongoDBQueryHelper.fullTextSearch(result, getCollectionId(), fullTextQuery,
                    MongoDBFormat.formatSearch(search),
                    MongoDBFormat.formatFieldsMask(fields), skip, max);
        }*/
    }

    @Override
    public Map<String, Object> collectionGet(Query search) throws NotFoundException, MoreThanOneFoundException {
        search.setCustom(MongoDBFormat.escapeInjections(search.getCustom()));
        return MongoDBQueryHelper.get(getTable().getCollectionId(),MongoDBFormat.formatSearch(search));
    }

    @Override
    public AggregationBean collectionAggregate(String field, Query search) {
        search.setCustom(MongoDBFormat.escapeInjections(search.getCustom()));
        return MongoDBAggregationHelper.aggregate(getTable().getCollectionId(),field,MongoDBFormat.formatSearch(search));
    }

    @Override
    public List<CountGroupBean> collectionCountGroup(String field, Query search) {
        search.setCustom(MongoDBFormat.escapeInjections(search.getCustom()));
        return MongoDBAggregationHelper.countGroup(getTable().getCollectionId(), field, MongoDBFormat.formatSearch(search));
    }

    @Override
    public void collectionAdd(String id, Map<String, Object> data) {
        MongoDBDDS.add(new Id(getTable().getCollectionId().getDatabase(),getTable().getCollectionId().getCollection(),id), data);
    }

    @Override
    public void collectionSet(String id, Map<String, Object> data) {
        MongoDBDDS.set(new Id(getTable().getCollectionId().getDatabase(), getTable().getCollectionId().getCollection(), id), data);
    }

    @Override
    public void collectionRemove(String id) {
        MongoDBDDS.remove(new Id(getTable().getCollectionId().getDatabase(), getTable().getCollectionId().getCollection(), id));
    }

}
