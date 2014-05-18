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
            ret = toMap(ret);
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

}
