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

package org.s1.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.s1.S1SystemError;
import org.s1.misc.Closure;
import org.s1.objects.ObjectIterator;
import org.s1.objects.Objects;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * MongoDB Format
 */
public class MongoDBFormat {

    /**
     *
     * @param obj
     * @return
     */
    public static Map<String,Object> toMap(DBObject obj){
        if(obj==null)
            return null;
        Map<String,Object> m = obj.toMap();
        /*if(m.get("_id")!=null) {
            m.put("_id", m.get("_id").toString());
        }*/
        m.remove("_id");

        m = Objects.iterate(m, new Closure<ObjectIterator.IterateBean, Object>() {
            @Override
            public Object call(ObjectIterator.IterateBean input) {
                Object o = input.getValue();
                if(o instanceof Map){
                    Map m = (Map)o;
                    if(m.containsKey("$date") && m.size()==1){
                        o = m.get("$date");
                    }
                    if(m.containsKey("_serializable") && m.size()==1){
                        try {
                            byte [] b = (byte [])m.get("_serializable");
                            ByteArrayInputStream bin = new ByteArrayInputStream(b);
                            ObjectInputStream ois = new ObjectInputStream(bin);
                            o = ois.readObject();
                        } catch (Exception e) {
                            throw S1SystemError.wrap(e);
                        }
                    }
                }
                return o;
            }
        });
        return m;
    }

    /**
     *
     * @param m
     * @return
     */
    public static DBObject fromMap(Map<String,Object> m){
        if(m==null)
            return null;
        m = Objects.iterate(m, new Closure<ObjectIterator.IterateBean, Object>() {
            @Override
            public Object call(ObjectIterator.IterateBean input) {
                Object o = input.getValue();
                if (o instanceof BigInteger) {
                    o = Objects.cast(o, Long.class);
                } else if (o instanceof BigDecimal) {
                    o = Objects.cast(o, Double.class);
                } else if (o instanceof Float) {
                    o = Objects.cast(o, Double.class);
                } else if(!(o instanceof List)
                        && !(o instanceof Map)
                        && !(o instanceof String)
                        && !(o instanceof Boolean)
                        && !(o instanceof Date)
                        && !(o instanceof Number)
                        && !(o instanceof ObjectId)
                        && !(o instanceof byte[])){
                    if(o instanceof Serializable){
                        try {
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            ObjectOutputStream oos = new ObjectOutputStream(bos);
                            oos.writeObject(o);
                            oos.flush();
                            o = Objects.newHashMap("_serializable",bos.toByteArray());
                        } catch (Exception e) {
                            throw S1SystemError.wrap(e);
                        }
                    }
                }
                return o;
            }
        });
        DBObject obj = new BasicDBObject(m);
        //try to convert to ObjectId
        /*if(m.get("_id")!=null) {
            try {
                m.put("_id", new ObjectId(""+m.get("_id")));
            } catch (Throwable e) {
            }
        }*/
        return obj;
    }

    /**
     * Remove $where commands from custom query (use it in Table)
     * @return
     */
    public static Map<String,Object> escapeInjections(Map<String,Object> m){
        if(m==null)
            return null;
        return Objects.iterate(m, new Closure<ObjectIterator.IterateBean, Object>() {
            @Override
            public Object call(ObjectIterator.IterateBean input) {
                if(input.getValue() instanceof Map){
                    Map<String,Object> o = (Map<String,Object>)input.getValue();
                    o.remove("$where");
                }
                return input.getValue();
            }
        });
    }
}
