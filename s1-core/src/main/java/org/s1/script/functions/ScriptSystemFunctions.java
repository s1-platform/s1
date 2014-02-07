package org.s1.script.functions;

import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.ObjectDiff;
import org.s1.objects.ObjectIterator;
import org.s1.objects.ObjectPath;
import org.s1.objects.Objects;
import org.s1.script.Context;
import org.s1.script.ScriptFunction;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 07.02.14
 * Time: 11:57
 */
public class ScriptSystemFunctions extends ScriptFunctions{

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

    public String substring(String s, Integer start, Integer end){
        return s.substring(start,end);
    }

    public String charAt(String s, Integer pos){
        return ""+s.charAt(pos);
    }

    public String toUpperCase(String s){
        return s.toUpperCase();
    }

    public String toLowerCase(String s){
        return s.toLowerCase();
    }

    public boolean startsWith(String s, String a){
        return s.startsWith(a);
    }

    public boolean endsWith(String s, String a){
        return s.endsWith(a);
    }

    public String replace(String s, String a, String b){
        return s.replace(a, b);
    }

    public String replaceAll(String s, String a, String b){
        return s.replaceAll(a, b);
    }

    public String replaceFirst(String s, String a, String b){
        return s.replaceFirst(a, b);
    }

    public List<String> split(String s, String a){
        return Objects.newArrayList(s.split(a));
    }

    public boolean matches(String s, String a){
        return s.matches(a);
    }

    public Map<String,Object> merge(List<Map<String,Object>> args){
        return Objects.merge(args);
    }

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

    public boolean contains(Object o, Object e){
        if(o instanceof String){
            return ((String) o).contains(Objects.cast(e, String.class));
        }else if(o instanceof List){
            return ((List) o).contains(e);
        }
        return false;
    }

    public int indexOf(Object o, Object e){
        if(o instanceof String){
            return ((String) o).indexOf(Objects.cast(e,String.class));
        }else if(o instanceof List){
            return ((List) o).indexOf(e);
        }
        return -1;
    }

    public int lastIndexOf(Object o, Object e){
        if(o instanceof String){
            return ((String) o).lastIndexOf(Objects.cast(e, String.class));
        }else if(o instanceof List){
            return ((List) o).lastIndexOf(e);
        }
        return -1;
    }

    public Date now(){
        return new Date();
    }

    public Date parseDate(String d, String format){
        return Objects.parseDate(d,format);
    }

    public String formatDate(Date d, String format){
        return Objects.formatDate(d, format);
    }

    public List<String> keys(Map<String,Object> m){
        List<String> l = Objects.newArrayList();
        for(String k:m.keySet()){
            l.add(k);
        }
        return l;
    }

    public List<Object> values(Map<String,Object> m){
        List<Object> l = Objects.newArrayList();
        for(Object k:m.values()){
            l.add(k);
        }
        return l;
    }

    public void add(List<Object> l, Object o){
        l.add(o);
    }

    public void addAll(List<Object> l, List<Object> o){
        l.addAll(o);
    }

    public void remove(List<Object> l, Integer i){
        l.remove(i.intValue());
    }

    public Object get(String path, Object def){
        String [] ts = ObjectPath.tokenizePath(path);
        if(ts.length>0){
            String t1 = ObjectPath.getLocalName(ts[0]);
            Map<String,Object> m = getContext().getMap(t1);
            if(m!=null){
                return Objects.get(m,path,def);
            }
        }
        return def;
    }

    public void set(String path, Object val){
        String [] ts = ObjectPath.tokenizePath(path);
        if(ts.length>0){
            String t1 = ObjectPath.getLocalName(ts[0]);
            Map<String,Object> m = getContext().getMap(t1);
            if(m!=null){
                Objects.set(m, path, val);
            }else{
                Objects.set(getContext().getVariables(), path, val);
            }
        }
    }
    
}
