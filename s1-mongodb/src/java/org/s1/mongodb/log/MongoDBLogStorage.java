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

package org.s1.mongodb.log;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.s1.log.LogStorage;
import org.s1.misc.Closure;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.objects.ObjectIterator;
import org.s1.objects.Objects;
import org.s1.options.Options;

import java.util.List;
import java.util.Map;

/**
 * MongoDB log storage impl
 */
public class MongoDBLogStorage extends LogStorage{

    public static DBCollection getCollection(){
        String d = "log4j";
        String c = Options.getStorage().get("MongoDB","connections.log4j.collection","log4j");
        return MongoDBConnectionHelper.getConnection(d).getCollection(c);
    }

    @Override
    public long list(List<Map<String, Object>> list, Map<String, Object> search, int skip, int max) {
        //return super.list(list, search, skip, max);
        DBCollection coll = getCollection();
        DBObject s = new BasicDBObject();
        if(search!=null){
            //remove $where
            search = Objects.iterate(search,new Closure<ObjectIterator.IterateBean, Object>() {
                @Override
                public Object call(ObjectIterator.IterateBean input) {
                    if(input.getValue() instanceof Map){
                        if(((Map) input.getValue()).containsKey("$where"))
                            ((Map) input.getValue()).remove("$where");
                    }
                    return input.getValue();
                }
            });
            s = new BasicDBObject(search);
        }
        DBCursor cur = coll
                .find(s).sort(new BasicDBObject("date",-1))
                .limit(max).skip(skip);

        while(cur.hasNext()){
            Map<String,Object> m = cur.next().toMap();
            m.remove("_id");
            list.add(m);
        }

        return coll.count(s);
    }
}
