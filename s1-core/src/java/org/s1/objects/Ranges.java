package org.s1.objects;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Range
 */
public class Ranges {

    public static String getNumberPrecision(float min, float max, int groups){
        return getNumberPrecision((double)min,(double)max,groups);
    }

    public static String getNumberPrecision(double min, double max, int groups){
        String f = "#,###.0";
        double step = (max-min)/groups;
        for(double i=step;i<0;i*=10){
            f+="0";
        }
        return f;
    }

    public static String getDatePrecision(Date min, Date max, int groups){
        Calendar cal = Calendar.getInstance();
        cal.setTime(min);
        cal.set(Calendar.MILLISECOND, 0);
        long step = Objects.cast(Math.ceil((max.getTime() - min.getTime()) / groups), Long.class);

        String f = "yyyy-MM-dd HH:mm:ss";
        if ((86400000L * 365) <= step) {
            f = "yyyy-MM";
        } else if ((86400000L * 30) <= step) {
            f = "yyyy-MM-dd";
        } else if ((86400000L * 7) <= step) {
            f = "yyyy-MM-dd";
        } else if (86400000L <= step) {
            f = "yyyy-MM-dd";
        } else if (3600000L <= step) {
            f = "yyyy-MM-dd HH:mm";
        } else if (60000L <= step) {
            f = "yyyy-MM-dd HH:mm";
        } else {

        }
        return f;
    }

    public static List<Integer> getIntRange(int min, int max, int groups){
        List<Long> res = getLongRange((long)min,(long)max,groups);
        List<Integer> res2 = Objects.newArrayList();
        for(Long l:res){
            res2.add(l.intValue());
        }
        return res2;
    }

    public static List<Long> getLongRange(long min, long max, int groups){
        List<Long> res = Objects.newArrayList();
        long step = 1;
        if((max-min)>groups){
            step = (max-min)/groups+((max-min)%groups==0?0:1);
        }
        if(max<min){
            if((min-max)>groups){
                step = (min-max)/groups+((min-max)%groups==0?0:1);
            }
            for (long i = min; i > max - step; i -= step) {
                res.add(i);
            }
        }else {
            for (long i = min; i < max + step; i += step) {
                res.add(i);
            }
        }
        return res;
    }

    public static List<Float> getFloatRange(float min, float max, int groups){
        List<Double> res = getDoubleRange((double)min,(double)max,groups);
        List<Float> res2 = Objects.newArrayList();
        for(Double l:res){
            res2.add(l.floatValue());
        }
        return res2;
    }

    public static List<Double> getDoubleRange(double min, double max, int groups){
        List<Double> res = Objects.newArrayList();
        double step = (max-min)/groups;
        if(min!=max) {
            for (int i = 0; i < groups; i++) {
                res.add(min);
                min += step;
            }
        }
        res.add(max);
        return res;
    }

    public static List<Date> getDateRange(Date min, Date max, int groups){
        boolean inverted = false;
        if(min.getTime()>max.getTime()){
            inverted = true;
            Date d = min;
            min = max;
            max = d;
        }
        List<Date> res = Objects.newArrayList();
        //
        Calendar cal = Calendar.getInstance();
        cal.setTime(min);
        cal.set(Calendar.MILLISECOND, 0);
        long step = Objects.cast(Math.ceil((max.getTime() - min.getTime()) / groups), Long.class);
        int calStep = Calendar.SECOND;
        int calStepSize = 1;
        if ((86400000L * 365) <= step) {
            calStep = Calendar.YEAR;
            cal.set(Calendar.MONTH, 0);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            for (int i = 0; i < 100; i++) {
                if ((86400000L * 365 * i) < step) {
                    calStepSize = i+1;
                } else
                    break;
            }
        } else if ((86400000L * 30) <= step) {
            calStep = Calendar.MONTH;
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            for (int i = 0; i < 12; i++) {
                if ((86400000L * 30 * i) < step) {
                    calStepSize = i+1;
                } else
                    break;
            }
        } else if ((86400000L * 7) <= step) {
            calStep = Calendar.WEEK_OF_YEAR;
            cal.set(Calendar.DAY_OF_WEEK, 1);
            cal.set(Calendar.HOUR, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.MILLISECOND, 0);
            for (int i = 0; i < 4; i++) {
                if ((86400000L * 7 * i) < step) {
                    calStepSize = i+1;
                } else
                    break;
            }
        } else if (86400000L <= step) {
            calStep = Calendar.DAY_OF_MONTH;
            cal.set(Calendar.HOUR, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            for (int i = 0; i < 7; i++) {
                if ((86400000L * i) < step) {
                    calStepSize = i+1;
                } else
                    break;
            }
        } else if (3600000L <= step) {
            calStep = Calendar.HOUR;
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            for (int i = 0; i < 24; i++) {
                if ((3600000L * i) < step) {
                    calStepSize = i+1;
                } else
                    break;
            }
        } else if (60000L <= step) {
            calStep = Calendar.MINUTE;
            cal.set(Calendar.SECOND, 0);
            for (int i = 0; i < 60; i++) {
                if ((60000L * i) < step) {
                    calStepSize = i+1;
                } else
                    break;
            }
        } else {
            for (int i = 0; i < 60; i++) {
                if ((1000L * i) < step) {
                    calStepSize = i+1;
                } else
                    break;
            }
        }

        //groups
        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(max);
        calEnd.add(calStep,calStepSize);
        for(;cal.getTime().getTime()<calEnd.getTime().getTime();cal.add(calStep,calStepSize)){
            Date dt = cal.getTime();
            res.add(dt);
        }
        if(inverted){
            Collections.reverse(res);
        }
        return res;
    }
}
