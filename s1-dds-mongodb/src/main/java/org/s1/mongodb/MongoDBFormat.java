package org.s1.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.s1.S1SystemError;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.ObjectIterator;
import org.s1.objects.Objects;
import org.s1.table.format.*;

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
     *
     * @param query
     * @return
     */
    public static DBObject formatSearch(Query query){
        DBObject o = new BasicDBObject();
        List<DBObject> l = Objects.newArrayList();
        if(query.getNode()!=null){
            l.add(formatQueryNode(query.getNode()));
        }
        if(query.getCustom()!=null){
            l.add(fromMap(query.getCustom()));
        }
        if(l.size()>0){
            o.put("$and",l);
        }

        return o;
    }

    /**
     *
     * @param qn
     * @return
     */
    public static DBObject formatQueryNode(QueryNode qn){
        DBObject o = new BasicDBObject();
        if(qn instanceof GroupQueryNode){
            List<DBObject> l = Objects.newArrayList();
            for(QueryNode n:((GroupQueryNode) qn).getChildren()){
                l.add(formatQueryNode(n));
            }
            if(l.size()>0)
                o.put(((GroupQueryNode) qn).getOperation()== GroupQueryNode.GroupOperation.OR?"$or":"$and",l);
        }else if(qn instanceof FieldQueryNode){
            FieldQueryNode.FieldOperation op = ((FieldQueryNode) qn).getOperation();
            String f = ((FieldQueryNode) qn).getField();
            Object val = ((FieldQueryNode) qn).getValue();
            if(op == FieldQueryNode.FieldOperation.EQUALS){
                o.put(f,val);
            }else if(op == FieldQueryNode.FieldOperation.CONTAINS){
                o.put(f, Pattern.compile(".*"+Pattern.quote(Objects.cast(val,String.class))+".*"));
            }else if(op == FieldQueryNode.FieldOperation.NULL){
                o.put(f,null);
            }else if(op == FieldQueryNode.FieldOperation.GT){
                o.put(f,new BasicDBObject("$gt",val));
            }else if(op == FieldQueryNode.FieldOperation.GTE){
                o.put(f,new BasicDBObject("$gte",val));
            }else if(op == FieldQueryNode.FieldOperation.LT){
                o.put(f,new BasicDBObject("$lt",val));
            }else if(op == FieldQueryNode.FieldOperation.LTE){
                o.put(f,new BasicDBObject("$lte",val));
            }
        }
        if(qn.isNot()){
            o = new BasicDBObject("$not",o);
        }
        return o;
    }

    /**
     *
     * @param sort
     * @return
     */
    public static DBObject formatSort(Sort sort){
        DBObject o = new BasicDBObject();
        if(sort!=null && !Objects.isNullOrEmpty(sort.getName())){
            o.put(sort.getName(),sort.isDesc()?-1:1);
        }
        return o;
    }

    /**
     *
     * @param fieldsMask
     * @return
     */
    public static DBObject formatFieldsMask(FieldsMask fieldsMask){
        DBObject o = new BasicDBObject();
        if(fieldsMask!=null){
            int i = fieldsMask.isShow()?1:0;
            for(String f: fieldsMask.getFields()){
                if(!Objects.isNullOrEmpty(f))
                    o.put(f,i);
            }
        }
        return o;
    }

}
