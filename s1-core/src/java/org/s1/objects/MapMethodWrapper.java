package org.s1.objects;

import org.s1.misc.Closure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * @author Grigory Pykhov
 */
public class MapMethodWrapper {
    private final static Logger LOG = LoggerFactory.getLogger(MapMethodWrapper.class);

    public static Object findAndInvoke(Object instance, String method, Map<String,Object> params) throws Exception{
        return invoke(instance,findMethod(instance,method),params);
    }

    public static Object findAndInvoke(Object instance, String method, List<Object> params) throws Exception{
        return invoke(instance,findMethod(instance,method),params);
    }

    public static Object findAndInvoke(Object instance, String method, Object ... params) throws Exception{
        return invoke(instance,findMethod(instance,method),params);
    }

    public static Method findMethod(Object instance, String method) throws MethodNotFoundException{
        Class cls = instance.getClass();
        Method mt = null;
        for (Class<?> c = cls; c != null; c = c.getSuperclass()){
            for(Method m:c.getDeclaredMethods()){
                if(m.getName().equals(method) && m.getAnnotation(MapMethod.class)!=null){
                    mt = m;
                    break;
                }
            }
            if(mt!=null)
                break;
        }

        if(mt!=null){
            return mt;
        } else{
            throw new MethodNotFoundException("Method " + method + " not found in class " + instance);
        }
    }

    public static Object toMap(Object obj){
        return Objects.iterate(Objects.newSOHashMap("object",obj),new Closure<ObjectIterator.IterateBean, Object>() {
            @Override
            public Object call(ObjectIterator.IterateBean input) {
                if(input.getValue() instanceof MapSerializableObject){
                    return ((MapSerializableObject) input.getValue()).toMap();
                }
                return input.getValue();
            }
        }).get("object");
    }

    protected static MapSerializableObject createFromMap(Object map, Class cls){
        if(MapSerializableObject.class.isAssignableFrom(cls)){
            MapSerializableObject m = null;
            try {
                m = ((Class<? extends MapSerializableObject>)cls).newInstance();
            } catch (Exception e) {
                LOG.warn("Cannot instantiate MapSerializableObject ("+cls+")");
                return null;
            }

            if(map instanceof Map){
                m.fromMap((Map<String,Object>)map);
            }

            return m;
        }
        return null;
    }

    public static Object fromMap(Object obj, Type type){
        if(type instanceof ParameterizedType){
            ParameterizedType pt = (ParameterizedType)type;
            if(List.class.isAssignableFrom((Class)pt.getRawType()) && obj!=null && obj instanceof List && pt.getActualTypeArguments().length>0){
                List l = (List)obj;
                for(int i=0;i<l.size();i++){
                    l.set(i, fromMap(l.get(i), pt.getActualTypeArguments()[0]));
                }
            }else if(Map.class.isAssignableFrom((Class)((ParameterizedType) type).getRawType()) && obj!=null && obj instanceof Map && pt.getActualTypeArguments().length>1){
                Map m = (Map)obj;
                List keys = Objects.newArrayList();
                for(Object k:m.keySet()){
                    keys.add(k);
                }
                for(Object k:keys){
                    Object v = m.get(k);
                    m.remove(k);
                    m.put(
                            fromMap(k, pt.getActualTypeArguments()[0]),
                            fromMap(v, pt.getActualTypeArguments()[1])
                    );
                }
            }
        }else if(type instanceof Class){
            if(MapSerializableObject.class.isAssignableFrom((Class)type)){
                return createFromMap(obj,(Class)type);
            }
        }
        return obj;
    }

    public static Object invoke(Object instance, Method method, Map<String,Object> params) throws Exception {
        if(params==null)
            params = Objects.newSOHashMap();
        List<Object> args = Objects.newArrayList();
        MapMethod ma = method.getAnnotation(MapMethod.class);

        for(int i=0;i<method.getParameterTypes().length;i++){
            args.add(params.get(ma.names()[i]));
        }
        return invoke(instance,method,args);
    }

    public static Object invoke(Object instance, Method method, List<Object> params) throws Exception {
        if(params==null)
            params = Objects.newArrayList();
        for(int i=0;i<method.getParameterTypes().length;i++){
            if(params.size()>i){
                Object o = params.get(i);
                Type t = method.getGenericParameterTypes()[i];
                o = fromMap(o,t);
                o = Objects.cast(o,method.getParameterTypes()[i]);
                params.set(i,o);
            }else{
                params.add(null);
            }
        }
        try {
            Object ret = method.invoke(instance, params.toArray());
            if(ret instanceof MapSerializableObject){
                ret = toMap((MapSerializableObject)ret);
            }
            return ret;
        }catch (InvocationTargetException e){
            if(e.getCause()!=null)
                throw (Exception)e.getCause();
            throw e;
        }
    }

    public static Object invoke(Object instance, Method method, Object ... params) throws Exception {
        return invoke(instance,method,Objects.newArrayList(params));
    }


    @MapMethod(names = {"a","b"})
    public void a(String a, int b){
        System.out.println(a+b);
    }

    @MapMethod
    public void b(String a,M1 b){
        System.out.println(a+":"+b.getA()+":"+b.getB());
    }

    @MapMethod
    public void c(String a,List<M1> b){
        for(M1 m:b) {
            System.out.println(a + "::" + m.getA() + ":" + m.getB());
        }
    }

    @MapMethod
    public void d(String a,List<List<M1>> b){
        for(List<M1> l:b) {
            for(M1 m:l) {
                System.out.println(a + "::" + m.getA() + ":" + m.getB());
            }
        }
    }

    @MapMethod
    public void map1(String a,Map<M1,List<M1>> b){
        for(M1 l:b.keySet()) {
            for(M1 m:b.get(l)) {
                System.out.println(a + "::" +
                                l.getA() + ":" + l.getB()+"->"+
                        m.getA() + ":" + m.getB());
            }
        }
    }

    public static class M1 implements MapSerializableObject{
        private int a;
        private String b;

        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }

        public String getB() {
            return b;
        }

        public void setB(String b) {
            this.b = b;
        }

        @Override
        public Map<String, Object> toMap() {
            return Objects.newSOHashMap("a",a,"b",b);
        }

        @Override
        public void fromMap(Map<String, Object> m) {
            a = Objects.get(m,"a");
            b = Objects.get(m,"b");
        }
    }

    public static void main(String[] args) throws Exception{
        MapMethodWrapper m = new MapMethodWrapper();
        //m.instances.put("test",m);
        //findAndInvoke(m,"a","qwer","1");
        //findAndInvoke(m,"b","qwer",Objects.newSOHashMap("a",2,"b","asd"));
        /*findAndInvoke(m,"c","qwer",Objects.newArrayList(
                Objects.newSOHashMap("a",1,"b","asd1"),
                Objects.newSOHashMap("a",2,"b","asd2")));
        */
        /*findAndInvoke(m,"d","qwer",Objects.newArrayList(
                Objects.newArrayList(
                Objects.newSOHashMap("a",1,"b","asd1")),
                Objects.newArrayList(
                        Objects.newSOHashMap("a", 2, "b", "asd2"))));*/
        findAndInvoke(m,"map1","qwer",Objects.newHashMap(
                Objects.newSOHashMap("a", 1, "b", "zxc1"),
                Objects.newArrayList(
                        Objects.newSOHashMap("a", 1, "b", "asd1")),
                Objects.newSOHashMap("a", 1, "b", "zxc2"),
                Objects.newArrayList(
                        Objects.newSOHashMap("a", 2, "b", "asd2"))
        ));
        //System.out.println(m.f);
        //invoke(m, method, Objects.newSOHashMap("a", "qwer", "b", 1));
    }

}
