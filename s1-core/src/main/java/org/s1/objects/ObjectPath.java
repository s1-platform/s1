package org.s1.objects;

import java.util.List;
import java.util.Map;

/**
 *
 */
public class ObjectPath {

    /**
     * Get value by path from object<br>
     * If not found return def
     *
     * @param olm
     * @param path
     * @param def
     * @param <T>
     * @return
     */
    static <T> T get(Map<String, Object> olm, String path, T def) {
        Object ret = def;
        try {
            String[] parts = tokenizePath(path);
            Object o = olm;
            for (int i = 0; i < parts.length; i++) {
                int[] j = getNumber(parts[i]);
                String name = getLocalName(parts[i]);
                o = ((Map) o).get(name);
                if (j != null) {
                    for (int k = 0; k < j.length; k++) {
                        o = ((List) o).get(j[k]);
                    }
                }
            }
            if (o != null)
                ret = o;
            else
                ret = def;
        } catch (Throwable e) {
        }
        return (T) ret;
    }

    static void set(Map<String, Object> olm, String path, Object val) {
        String[] parts = tokenizePath(path);
        Map<String,Object> o = olm;
        for (int i = 0; i < parts.length; i++) {
            int[] j = getNumber(parts[i]);
            String name = getLocalName(parts[i]);
            if(i==parts.length-1){
                if(j!=null){
                    if(!o.containsKey(name)){
                        o.put(name,Objects.newArrayList());
                    }
                    List<Object> o1 = (List<Object>)o.get(name);

                    for (int k = 0; k < j.length; k++) {
                        if(o1.size()<=j[k]){
                            for(int ii=0;ii<=j[k]-o1.size();ii++)
                                o1.add(null);
                            if(k==j.length-1){
                                o1.set(j[k],Objects.newHashMap());
                            }else{
                                o1.set(j[k],Objects.newArrayList());
                            }
                        }
                        if(k==j.length-1){
                            o1.set(j[k],val);
                        }else{
                            o1 = (List<Object>)o1.get(j[k]);
                        }
                    }
                }else{
                    o.put(name,val);
                }
            }else{

                if(j!=null){
                    if(!o.containsKey(name)){
                        o.put(name,Objects.newArrayList());
                    }
                    List<Object> o1 = (List<Object>)o.get(name);

                    for (int k = 0; k < j.length; k++) {
                        if(o1.size()<=j[k]){
                            for(int ii=o1.size();ii<=j[k];ii++){
                                o1.add(null);
                            }
                            if(k==j.length-1){
                                o1.set(j[k], Objects.newHashMap());
                            }else{
                                o1.set(j[k],Objects.newArrayList());
                            }
                        }
                        if(k==j.length-1){
                            o = (Map<String,Object>)o1.get(j[k]);
                        }else{
                            o1 = (List<Object>)o1.get(j[k]);
                        }
                    }
                }else{
                    if(!o.containsKey(name)){
                        o.put(name,Objects.newHashMap());
                    }
                    o = (Map<String,Object>)o.get(name);
                }
            }
        }
    }

    /**
     * @param path
     * @return
     */
    public static String[] tokenizePath(String path) {
        String s = path;
        s = s.replace("&", "&amp;");
        s = s.replace("\\\\", "&backslash;");
        s = s.replace("\\.", "&dot;");
        String[] p = s.split("\\.");
        String[] p2 = new String[p.length];
        for (int i = 0; i < p.length; i++) {
            p2[i] = p[i]
                    .replace("&dot;", ".")
                    .replace("&backslash;", "\\\\")
                    .replace("&amp;", "&");
        }
        return p2;
    }

    /**
     * @param name
     * @return
     */
    public static int[] getNumber(String name) {
        String s = name;
        s = s.replace("&", "&amp;");
        s = s.replace("\\\\", "&backslash;");
        s = s.replace("\\[", "&open;");
        s = s.replace("\\]", "&close;");
        if (s.indexOf("[") < s.indexOf("]")) {
            String s1 = s.substring(s.indexOf("[") + 1, s.lastIndexOf("]"));
            String[] s2 = s1.split("\\]\\[");
            int[] r = new int[s2.length];
            for (int i = 0; i < s2.length; i++) {
                r[i] = Integer.parseInt(s2[i]);
            }
            return r;
        }
        return null;
    }

    /**
     * @param name
     * @return
     */
    public static String getLocalName(String name) {
        String s = name;
        s = s.replace("&", "&amp;");
        s = s.replace("\\\\", "&backslash;");
        s = s.replace("\\[", "&open;");
        s = s.replace("\\]", "&close;");
        String s1 = s;
        if (s.indexOf("[") < s.indexOf("]")) {
            s1 = s.substring(0, s.indexOf("["));
        }
        name = s1.replace("&open;", "[").replace("&close;", "]")
                .replace("&backslash;", "\\")
                .replace("&amp;", "&");
        //name = name.replace("\\[", "[").replace("\\]", "]");
        return name;
    }
}
