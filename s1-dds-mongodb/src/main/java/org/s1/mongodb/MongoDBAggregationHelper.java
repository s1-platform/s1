package org.s1.mongodb;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.s1.objects.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * MongoDB aggregation helper
 * DEBUG - aggregate, countGroup
 */
public class MongoDBAggregationHelper {

    private static final Logger LOG = LoggerFactory.getLogger(MongoDBAggregationHelper.class);

    /**
     * Aggregate by field
     *
     * @param instance
     * @param collection
     * @param field
     * @param search
     * @return {max,min,sum,count,avg}
     */
    public static Map<String, Object> aggregate(String instance, String collection, String field,
                             Map<String, Object> search) {
        DBCollection coll = MongoDBConnectionHelper.getConnection(instance).getCollection(collection);

        Map<String,Object> gr = Objects.newHashMap(
                "_id","$_null",
                "min",Objects.newHashMap("$min","$"+field),
                "max",Objects.newHashMap("$max","$"+field),
                "sum",Objects.newHashMap("$sum","$"+field),
                "avg",Objects.newHashMap("$avg","$"+field),
                "count",Objects.newHashMap("$sum",1L)
        );

        //search
        BasicDBObject s = new BasicDBObject();
        if (search != null)
            s = new BasicDBObject(search);

        AggregationOutput out = coll.aggregate(new BasicDBObject("$match", s),
                new BasicDBObject("$group", gr));

        List<Map<String, Object>> l = (List<Map<String, Object>>) out.getCommandResult().get("result");
        Map<String,Object> res = Objects.newHashMap();
        if(l.size()>0){
            res = l.get(0);
            res.remove("_id");
        }
        if(LOG.isDebugEnabled())
            LOG.debug("MongoDB aggregation result ("+"instance: "+instance+", collection: "+collection+", search: "+search+
                    ") \n\t> "+res);
        return res;
    }

    /**
     * Average group count in countGroup
     * Default = 20
     */
    public static int GROUP_COUNT = 20;

    /**
     * Get groups (by field) with record count.
     * Performs auto regroup if group count is larger then GROUP_COUNT
     *
     * @param instance
     * @param collection
     * @param field
     * @param search
     * @return [{_id, count},...]
     */
    public static List<Map<String, Object>> countGroup(String instance, String collection,
                                                       String field, Map<String, Object> search){


        DBCollection coll = MongoDBConnectionHelper.getConnection(instance).getCollection(collection);
        Map<String,Object> group = Objects.newHashMap(
                "_id","$"+field,
                "count",Objects.newHashMap("$sum",1L)
        );

        //search
        BasicDBObject s = new BasicDBObject();
        if (search != null)
            s = new BasicDBObject(search);

        AggregationOutput out = coll.aggregate(new BasicDBObject("$match", s),
                new BasicDBObject("$group", group));

        List<Map<String,Object>> result = (List<Map<String, Object>>) out.getCommandResult().get("result");

        //regroup
        if (result.size() > GROUP_COUNT) {
            final Object probe = result.get(0).get("_id");

            if(LOG.isDebugEnabled())
                LOG.debug("Count group probe: "+probe.getClass().getName());
            List<Map<String,Object>> cuttedResult = Objects.newArrayList();
            if (!(probe instanceof Number || probe instanceof Date)) {
                //sort by count desc
                Collections.sort(result, new Comparator<Map<String, Object>>() {
                    @Override
                    public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                        long count1 = Objects.get(Long.class,o1,"count");
                        long count2 = Objects.get(Long.class,o2,"count");
                        if(count1>count2){
                            return -1;
                        }else if(count1==count2){
                            return 0;
                        }else{
                            return 1;
                        }
                    }
                });

                //total count
                long valSum = 0;
                for(Map<String,Object> o:result){
                    long count = Objects.get(Long.class,o,"count");
                    valSum+=count;
                }

                //first 20 count
                long firstValSum = 0;
                for(int i=0; i<GROUP_COUNT;i++){
                    long count = Objects.get(Long.class,result.get(i),"count");
                    firstValSum+=count;
                }

                //show first 20, and "others"
                for (int i = 0; i < GROUP_COUNT; i++) {
                    cuttedResult.add(result.get(i));
                }
                cuttedResult.add(Objects.newHashMap(String.class,Object.class, "other", valSum - firstValSum));
            } else {
                List<Map<String,Object>> result2 = Objects.newArrayList();
                for(Map<String,Object> o:result){
                    if(o.get("_id")!=null){
                        result2.add(o);
                    }
                }
                result = result2;

                //min, max
                Collections.sort(result,new Comparator<Map<String, Object>>() {
                    @Override
                    public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                        if(probe instanceof Date){
                            long n1 = Objects.get(Date.class, o1,"_id").getTime();
                            long n2 = Objects.get(Date.class, o2,"_id").getTime();
                            if(n1>n2){
                                return 1;
                            }else if(n1==n2){
                                return 0;
                            }else{
                                return -1;
                            }
                        }else if(probe instanceof Number){
                            double n1 = Objects.get(Number.class,o1,"_id").doubleValue();
                            double n2 = Objects.get(Number.class,o2,"_id").doubleValue();
                            if(n1>n2){
                                return 1;
                            }else if(n1==n2){
                                return 0;
                            }else{
                                return -1;
                            }
                        }
                        return 0;
                    }
                });

                Object max = result.get(result.size() - 1).get("_id");
                Object min = result.get(0).get("_id");

                Map<String,Long> resultMap = Objects.newHashMap();
                //make range
                if (probe instanceof Date) {
                    //
                    Calendar cal = Calendar.getInstance();
                    cal.setTime((Date)min);
                    cal.set(Calendar.MILLISECOND, 0);
                    long step = Objects.cast(Math.ceil((((Date) max).getTime() - ((Date) min).getTime()) / GROUP_COUNT), Long.class);
                    if(LOG.isDebugEnabled())
                        LOG.debug("Step: "+step+", min: "+min+", max: "+max);

                    int calStep = Calendar.SECOND;
                    int calStepSize = 1;
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                    if ((86400000L * 365) <= step) {
                        calStep = Calendar.YEAR;
                        cal.set(Calendar.MONTH, 0);
                        cal.set(Calendar.DAY_OF_MONTH, 1);
                        cal.set(Calendar.HOUR, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        for (int i = 1; i < 100; i++) {
                            if ((86400000L * 365 * i) <= step) {
                                calStepSize = i;
                            } else
                                break;
                        }
                        sdf = new SimpleDateFormat("MM.yyyy");
                    } else if ((86400000L * 30) <= step) {
                        calStep = Calendar.MONTH;
                        cal.set(Calendar.DAY_OF_MONTH, 1);
                        cal.set(Calendar.HOUR, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        for (int i = 1; i < 13; i++) {
                            if ((86400000L * 30 * i) <= step) {
                                calStepSize = i;
                            } else
                                break;
                        }
                        sdf = new SimpleDateFormat("dd.MM.yyyy");
                    } else if ((86400000L * 7) <= step) {
                        calStep = Calendar.WEEK_OF_YEAR;
                        cal.set(Calendar.DAY_OF_WEEK, 1);
                        cal.set(Calendar.HOUR, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        for (int i = 1; i < 5; i++) {
                            if ((86400000L * 7 * i) <= step) {
                                calStepSize = i;
                            } else
                                break;
                        }
                        sdf = new SimpleDateFormat("dd.MM.yyyy");
                    } else if (86400000L <= step) {
                        calStep = Calendar.DAY_OF_MONTH;
                        cal.set(Calendar.HOUR, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        for (int i = 1; i < 8; i++) {
                            if ((86400000L * i) <= step) {
                                calStepSize = i;
                            } else
                                break;
                        }
                        sdf = new SimpleDateFormat("dd.MM.yyyy");
                    } else if (3600000L <= step) {
                        calStep = Calendar.HOUR;
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        for (int i = 1; i < 25; i++) {
                            if ((3600000L * i) <= step) {
                                calStepSize = i;
                            } else
                                break;
                        }
                        sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                    } else if (60000L <= step) {
                        calStep = Calendar.MINUTE;
                        cal.set(Calendar.SECOND, 0);
                        for (int i = 1; i < 61; i++) {
                            if ((60000L * 365 * i) <= step) {
                                calStepSize = i;
                            } else
                                break;
                        }
                        sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                    } else {
                        for (int i = 1; i < 61; i++) {
                            if ((1000L * i) <= step) {
                                calStepSize = i;
                            } else
                                break;
                        }
                    }
                    if(LOG.isDebugEnabled())
                        LOG.debug("Normalized step: "+calStep+", step size: "+calStepSize);

                    cal.add(calStep, calStepSize);
                    //group by
                    for(Map<String,Object> o:result){
                        long cnt = Objects.get(o,"count");
                        Date _id = Objects.get(Date.class,o,"_id");
                        if (cal.getTimeInMillis() <= _id.getTime()) {
                            cal.add(calStep, calStepSize);
                        }
                        Calendar cal1 = Calendar.getInstance();
                        cal1.setTime(cal.getTime());
                        Date to = cal1.getTime();
                        cal1.add(calStep, -calStepSize);
                        Date from = cal1.getTime();
                        String str = ""+sdf.format(from) + " - " + sdf.format(to);

                        if(resultMap.containsKey(str)){
                            long c = resultMap.get(str);
                            resultMap.put(str,cnt+c);
                        }else{
                            resultMap.put(str,cnt);
                        }
                    }
                } else if (probe instanceof Long || probe instanceof Integer) {
                    long min_d = Objects.cast(min,Long.class);
                    long max_d = Objects.cast(max,Long.class);

                    //
                    long step = Objects.cast(Math.ceil((max_d - min_d) / GROUP_COUNT),Long.class);
                    if(LOG.isDebugEnabled())
                        LOG.debug("Step: "+step+", min: "+min+", max: "+max);

                    for(Map<String,Object> o:result){
                        long cnt = Objects.get(o,"count");
                        long _id = Objects.get(Long.class,o,"_id");
                        long gr = min_d;
                        for (int i = 0; i < GROUP_COUNT; i++) {
                            if (gr + step > _id) {
                                break;
                            }
                            gr += step;
                        }
                        String str = ""+gr + " - " + (gr + step);

                        if(resultMap.containsKey(str)){
                            long c = resultMap.get(str);
                            resultMap.put(str,cnt+c);
                        }else{
                            resultMap.put(str,cnt);
                        }
                    }
                } else if (probe instanceof Double || probe instanceof Float) {
                    double min_d = Objects.cast(min,Double.class);
                    double max_d = Objects.cast(max,Double.class);

                    //
                    double step = (max_d - min_d) / GROUP_COUNT;
                    if(LOG.isDebugEnabled())
                        LOG.debug("Step: "+step+", min: "+min+", max: "+max);

                    for(Map<String,Object> o:result){
                        long cnt = Objects.get(o,"count");
                        double _id = Objects.get(Double.class,o,"_id");
                        double gr = min_d;
                        for (int i = 0; i < GROUP_COUNT; i++) {
                            if (gr + step > _id) {
                                break;
                            }
                            gr += step;
                        }
                        String str = ""+gr + " - " + (gr + step);

                        if(resultMap.containsKey(str)){
                            long c = resultMap.get(str);
                            resultMap.put(str,cnt+c);
                        }else{
                            resultMap.put(str,cnt);
                        }
                    }
                }
                for(String str:resultMap.keySet() ){
                    cuttedResult.add(Objects.newHashMap(String.class,Object.class,
                            "_id",str,"count",resultMap.get(str)
                    ));
                }
            }

            result = cuttedResult;
        }

        if(LOG.isDebugEnabled())
            LOG.debug("MongoDB countGroup result ("+"instance: "+instance+", collection: "+collection+", search: "+search+
                    ") \n\t> "+result);
        return result;
    }


}
