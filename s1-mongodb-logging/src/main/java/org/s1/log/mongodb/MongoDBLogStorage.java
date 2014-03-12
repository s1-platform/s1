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

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.s1.log.LogStorage;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.ObjectIterator;
import org.s1.objects.Objects;

import java.util.List;
import java.util.Map;

/**
 * MongoDB log storage impl
 */
public class MongoDBLogStorage extends LogStorage{

    public MongoDBLogStorage() {
        DBCollection coll = MongoDBLogConnectionHelper.getCollection();
    }

    @Override
    public long list(List<Map<String, Object>> list, Map<String, Object> search, int skip, int max) {
        //return super.list(list, search, skip, max);

        DBObject s = new BasicDBObject();
        if(search!=null){
            //remove $where
            search = Objects.iterate(search,new Closure<ObjectIterator.IterateBean, Object>() {
                @Override
                public Object call(ObjectIterator.IterateBean input) throws ClosureException {
                    if(input.getValue() instanceof Map){
                        if(((Map) input.getValue()).containsKey("$where"))
                            ((Map) input.getValue()).remove("$where");
                    }
                    return input.getValue();
                }
            });
            s = new BasicDBObject(search);
        }
        DBCursor cur = MongoDBLogConnectionHelper.getCollection().find(s).sort(new BasicDBObject("date",-1))
                .limit(max).skip(skip);

        while(cur.hasNext()){
            Map<String,Object> m = cur.next().toMap();
            m.remove("_id");
            list.add(m);
        }

        return MongoDBLogConnectionHelper.getCollection().count(s);
    }
}
