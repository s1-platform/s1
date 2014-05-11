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

package org.s1.script.function;

import org.s1.cluster.Session;
import org.s1.format.json.JSONFormat;
import org.s1.format.json.JSONFormatException;
import org.s1.misc.Closure;
import org.s1.objects.*;
import org.s1.user.UserBean;
import org.s1.user.Users;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * System script functions (available in all scripts)
 */
public class SystemFunctionSet extends ScriptFunctionSet {

    /**
     * Get string|list|map length
     *
     * @param o
     * @return
     */
    @MapMethod
    public int length(Object o){
        if(o instanceof List){
            return ((List) o).size();
        }else if(o instanceof Map){
            return ((Map) o).size();
        }else if(o instanceof String){
            return ((String) o).length();
        }
        return 0;
    }

    /**
     *
     * @param s
     * @param start
     * @param end
     * @return
     */
    @MapMethod
    public String substring(String s, Integer start, Integer end){
        return s.substring(start,end);
    }

    /**
     *
     * @param s
     * @param pos
     * @return
     */
    @MapMethod
    public String charAt(String s, Integer pos){
        return ""+s.charAt(pos);
    }

    /**
     *
     * @param s
     * @return
     */
    @MapMethod
    public String toUpperCase(String s){
        return s.toUpperCase();
    }

    /**
     *
     * @param s
     * @return
     */
    @MapMethod
    public String toLowerCase(String s){
        return s.toLowerCase();
    }

    /**
     *
     * @param s
     * @param a
     * @return
     */
    @MapMethod
    public boolean startsWith(String s, String a){
        return s.startsWith(a);
    }

    /**
     *
     * @param s
     * @param a
     * @return
     */
    @MapMethod
    public boolean endsWith(String s, String a){
        return s.endsWith(a);
    }

    /**
     *
     * @param s source
     * @param a replace string
     * @param b replace with
     * @return
     */
    @MapMethod
    public String replace(String s, String a, String b){
        return s.replace(a, b);
    }

    /**
     *
     * @param s source
     * @param a regex
     * @param b replace with
     * @return
     */
    @MapMethod
    public String replaceAll(String s, String a, String b){
        return s.replaceAll(a, b);
    }

    /**
     *
     * @param s source
     * @param a regex
     * @param b replace with
     * @return
     */
    @MapMethod
    public String replaceFirst(String s, String a, String b){
        return s.replaceFirst(a, b);
    }

    /**
     *
     * @param s source
     * @param a regex
     * @return
     */
    @MapMethod
    public List<String> split(String s, String a){
        return Objects.newArrayList(s.split(a));
    }

    /**
     *
     * @param s source
     * @param a regex
     * @return
     */
    @MapMethod
    public boolean matches(String s, String a){
        return s.matches(a);
    }

    /**
     * @see org.s1.objects.Objects#merge(java.util.List)
     *
     * @param args
     * @return
     */
    @MapMethod
    public Map<String,Object> merge(List<Map<String,Object>> args){
        return Objects.merge(args);
    }

    /**
     * @see org.s1.objects.Objects#diff(java.util.Map, java.util.Map)
     *
     * @param old
     * @param nw
     * @return
     */
    @MapMethod
    public List<Map<String,Object>> diff(Map<String,Object> old, Map<String,Object> nw){
        List<ObjectDiff.DiffBean> l = Objects.diff(old,nw);
        List<Map<String,Object>> list = Objects.newArrayList();
        for(ObjectDiff.DiffBean b:l){
            list.add(Objects.newHashMap(String.class,Object.class,
                    "path",b.getPath(),"old",b.getOldValue(),"new",b.getNewValue()
            ));
        }
        return list;
    }

    /**
     * @see org.s1.objects.Objects#iterate(java.util.Map, org.s1.misc.Closure)
     *
     * @param o
     * @param f function(path,value,name){...}
     * @return
     */
    @MapMethod
    public Map<String,Object> iterate(Map<String,Object> o, final ScriptFunction f){
        return Objects.iterate(o,new Closure<ObjectIterator.IterateBean, Object>() {
            @Override
            public Object call(ObjectIterator.IterateBean input) {
                return f.call(Objects.newHashMap(String.class,Object.class,
                        "path",input.getPath(),
                        "value",input.getValue(),
                        "name",input.getName()
                        ));
            }
        });
    }

    @MapMethod
    public Map<String,Object> toWire(Map<String,Object> o){
        return Objects.toWire(o);
    }

    @MapMethod
    public Map<String,Object> fromWire(Map<String,Object> o){
        return Objects.fromWire(o);
    }

    @MapMethod
    public String toJSON(Map<String,Object> o){
        return JSONFormat.toJSON(o);
    }

    @MapMethod
    public Map<String,Object> evalJSON(String json) throws JSONFormatException {
        return JSONFormat.evalJSON(json);
    }

    @MapMethod
    public List asList(Object o, Boolean evenIfNull) throws JSONFormatException {
        if(evenIfNull==null)
            return Objects.asList(o);
        return Objects.asList(o,evenIfNull);
    }

    /**
     *
     * @param o list|string
     * @param e
     * @return
     */
    @MapMethod
    public boolean contains(Object o, Object e){
        if(o instanceof String){
            return ((String) o).contains(Objects.cast(e, String.class));
        }else if(o instanceof List){
            return ((List) o).contains(e);
        }
        return false;
    }

    /**
     *
     * @param o list|string
     * @param e
     * @return
     */
    @MapMethod
    public int indexOf(Object o, Object e){
        if(o instanceof String){
            return ((String) o).indexOf(Objects.cast(e,String.class));
        }else if(o instanceof List){
            return ((List) o).indexOf(e);
        }
        return -1;
    }

    /**
     *
     * @param o list|string
     * @param e
     * @return
     */
    @MapMethod
    public int lastIndexOf(Object o, Object e){
        if(o instanceof String){
            return ((String) o).lastIndexOf(Objects.cast(e, String.class));
        }else if(o instanceof List){
            return ((List) o).lastIndexOf(e);
        }
        return -1;
    }

    /**
     * New date
     *
     * @return
     */
    @MapMethod
    public Date now(){
        return new Date();
    }

    /**
     * @see Objects#parseDate(String, String)
     *
     * @param d
     * @param format
     * @return
     */
    @MapMethod
    public Date parseDate(String d, String format){
        return Objects.parseDate(d,format);
    }

    /**
     * @see Objects#formatDate(java.util.Date, String)
     *
     * @param d
     * @param format
     * @return
     */
    @MapMethod
    public String formatDate(Date d, String format){
        return Objects.formatDate(d, format);
    }

    @MapMethod
    public Double parseNumber(Object n){
        return Objects.cast(n, Double.class);
    }

    @MapMethod
    public String formatNumber(Object n, String groupSeparator,String decimalSeparator, String format){
        if(Objects.isNullOrEmpty(groupSeparator))
            groupSeparator=",";
        if(Objects.isNullOrEmpty(decimalSeparator))
            decimalSeparator=".";
        if(Objects.isNullOrEmpty(format))
            format="###";
        DecimalFormat df = new DecimalFormat(format);
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator(decimalSeparator.charAt(0));
        dfs.setGroupingSeparator(groupSeparator.charAt(0));
        dfs.setMonetaryDecimalSeparator(decimalSeparator.charAt(0));
        df.setDecimalFormatSymbols(dfs);
        return df.format(Objects.cast(n, BigDecimal.class));
    }

    /**
     *
     * @param o
     */
    @MapMethod
    public void clear(Object o){
        if(o instanceof Map){
            ((Map) o).clear();
        }else if(o instanceof List){
            ((List) o).clear();
        }
    }

    /**
     * Put all
     *
     * @param m
     * @param m2
     */
    @MapMethod
    public void putAll(Map<String,Object> m, Map<String,Object> m2){
        m.putAll(m2);
    }

    /**
     * Map keys
     *
     * @param m
     * @return
     */
    @MapMethod
    public List<String> keys(Map<String,Object> m){
        List<String> l = Objects.newArrayList();
        for(String k:m.keySet()){
            l.add(k);
        }
        return l;
    }

    /**
     * Map values
     *
     * @param m
     * @return
     */
    @MapMethod
    public List<Object> values(Map<String,Object> m){
        List<Object> l = Objects.newArrayList();
        for(Object k:m.values()){
            l.add(k);
        }
        return l;
    }

    /**
     *
     * @param l
     * @param o
     */
    @MapMethod
    public void add(List<Object> l, Object o){
        getContext().getMemoryHeap().take(o);
        l.add(o);
    }

    /**
     *
     * @param l
     * @param o
     */
    @MapMethod
    public void addAll(List<Object> l, List<Object> o){
        getContext().getMemoryHeap().take(o);
        l.addAll(o);
    }

    @MapMethod
    public void remove(List<Object> l, Integer i){
        l.remove(i.intValue());
    }

    /**
     *
     * @param m
     * @param path
     * @param def
     * @return
     */
    @MapMethod
    public Object get(Map<String,Object> m, String path, Object def){
        return Objects.get(m,path,def);
    }

    /**
     *
     * @param m
     * @param path
     * @param val
     */
    @MapMethod
    public void set(Map<String,Object> m, String path, Object val){
        getContext().getMemoryHeap().take(path);
        getContext().getMemoryHeap().take(val);
        Objects.set(m, path, val);
    }

    /**
     *
     * @return
     */
    @MapMethod
    public Map<String,Object> whoAmI() {
        String id = Session.getSessionBean().getUserId();
        return Users.getUser(id);
    }

    /**
     *
     * @return
     */
    @MapMethod
    public boolean inRole(String role) {
        String id = Session.getSessionBean().getUserId();
        UserBean ub = Users.getUser(id);
        return Users.isUserInRole(ub,role);
    }

    @MapMethod
    public List<Long> getLongRange(Long min, Long max, Integer groups){
        return Ranges.getLongRange(Objects.cast(min,Long.class),Objects.cast(max,Long.class),groups);
    }

    @MapMethod
    public List<Date> getDateRange(Date min, Date max, Integer groups){
        return Ranges.getDateRange(Objects.cast(min,Date.class),Objects.cast(max,Date.class),groups);
    }

    @MapMethod
    public List<Double> getDoubleRange(Double min, Double max, Integer groups){
        return Ranges.getDoubleRange(Objects.cast(min,Double.class),Objects.cast(max,Double.class),groups);
    }
    
}
