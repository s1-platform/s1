package org.s1.script.functions;

import org.s1.cluster.Session;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.ObjectDiff;
import org.s1.objects.ObjectIterator;
import org.s1.objects.ObjectPath;
import org.s1.objects.Objects;
import org.s1.script.Context;
import org.s1.script.ScriptFunction;
import org.s1.user.UserBean;
import org.s1.user.Users;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * System script functions (available in all scripts)
 */
public class ScriptSystemFunctions extends ScriptFunctions{

    /**
     * Get string|list|map length
     *
     * @param o
     * @return
     */
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
    public String substring(String s, Integer start, Integer end){
        return s.substring(start,end);
    }

    /**
     *
     * @param s
     * @param pos
     * @return
     */
    public String charAt(String s, Integer pos){
        return ""+s.charAt(pos);
    }

    /**
     *
     * @param s
     * @return
     */
    public String toUpperCase(String s){
        return s.toUpperCase();
    }

    /**
     *
     * @param s
     * @return
     */
    public String toLowerCase(String s){
        return s.toLowerCase();
    }

    /**
     *
     * @param s
     * @param a
     * @return
     */
    public boolean startsWith(String s, String a){
        return s.startsWith(a);
    }

    /**
     *
     * @param s
     * @param a
     * @return
     */
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
    public String replaceFirst(String s, String a, String b){
        return s.replaceFirst(a, b);
    }

    /**
     *
     * @param s source
     * @param a regex
     * @return
     */
    public List<String> split(String s, String a){
        return Objects.newArrayList(s.split(a));
    }

    /**
     *
     * @param s source
     * @param a regex
     * @return
     */
    public boolean matches(String s, String a){
        return s.matches(a);
    }

    /**
     * @see org.s1.objects.Objects#merge(java.util.List)
     *
     * @param args
     * @return
     */
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
    public Map<String,Object> iterate(Map<String,Object> o, final ScriptFunction f){
        return Objects.iterate(o,new Closure<ObjectIterator.IterateBean, Object>() {
            @Override
            public Object call(ObjectIterator.IterateBean input) throws ClosureException {
                return f.call(Objects.newHashMap(String.class,Object.class,
                        "path",input.getPath(),
                        "value",input.getValue(),
                        "name",input.getName()
                        ));
            }
        });
    }

    /**
     *
     * @param o list|string
     * @param e
     * @return
     */
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
    public String formatDate(Date d, String format){
        return Objects.formatDate(d, format);
    }

    /**
     *
     * @param o
     */
    public void clear(Object o){
        if(o instanceof Map){
            ((Map) o).clear();
        }else if(o instanceof List){
            ((List) o).clear();
        }
    }

    /**
     * Map keys
     *
     * @param m
     * @return
     */
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
    public void add(List<Object> l, Object o){
        getContext().getMemoryHeap().take(o);
        l.add(o);
    }

    /**
     *
     * @param l
     * @param o
     */
    public void addAll(List<Object> l, List<Object> o){
        getContext().getMemoryHeap().take(o);
        l.addAll(o);
    }

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
    public Object get(Map<String,Object> m, String path, Object def){
        return Objects.get(m,path,def);
    }

    /**
     *
     * @param m
     * @param path
     * @param val
     */
    public void set(Map<String,Object> m, String path, Object val){
        getContext().getMemoryHeap().take(path);
        getContext().getMemoryHeap().take(val);
        Objects.set(m, path, val);
    }

    /**
     *
     * @return
     */
    public Map<String,Object> whoAmI() throws NotFoundException{
        String id = Session.getSessionBean().getUserId();
        return Users.getUser(id);
    }

    /**
     *
     * @return
     */
    public boolean inRole(String role) throws NotFoundException{
        String id = Session.getSessionBean().getUserId();
        UserBean ub = Users.getUser(id);
        return Users.isUserInRole(ub,role);
    }
    
}
