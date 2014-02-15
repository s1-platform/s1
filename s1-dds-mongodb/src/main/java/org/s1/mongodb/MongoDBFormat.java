package org.s1.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.s1.S1SystemError;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.ObjectIterator;
import org.s1.objects.Objects;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
        if(m.get("_id")!=null)
            m.put("_id",m.get("_id").toString());

        m = Objects.iterate(m, new Closure<ObjectIterator.IterateBean, Object>() {
            @Override
            public Object call(ObjectIterator.IterateBean input) throws ClosureException {
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
                /*if(o instanceof Integer || o instanceof Long){
                    o = Objects.cast(o,BigInteger.class);
                }else if(o instanceof Float || o instanceof Double){
                    o = Objects.cast(o,BigDecimal.class);
                }*/
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
            public Object call(ObjectIterator.IterateBean input) throws ClosureException {
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
        if(m.get("_id")!=null){
            obj.put("_id",new ObjectId(""+m.get("_id")));
        }
        return obj;
    }

    /**
     * Removes where to prevent possible injection
     * @param query
     * @return
     */
    public static Map<String,Object> removeWhere(Map<String,Object> query){
        return Objects.iterate(query, new Closure<ObjectIterator.IterateBean, Object>() {
            @Override
            public Object call(ObjectIterator.IterateBean input) throws ClosureException {
                Object o = input.getValue();
                if(o instanceof Map && ((Map)o).containsKey("$where")){
                    ((Map)o).remove("$where");
                }
                return o;
            }
        });
    }

}
