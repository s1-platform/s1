package org.s1.objects;

import org.s1.misc.Closure;

import java.util.List;
import java.util.Map;

/**
 * Object Merge helper
 */
public class ObjectMerge {

    /**
     * Merge maps right map override left map
     *
     * @param args
     * @return
     */
    static Map<String, Object> merge(List<Map<String, Object>> args) {
        if (args.size() > 1) {
            Map<String, Object> obj = args.get(0);
            for (int i = 1; i < args.size(); i++) {
                obj = mergeTwo(obj, args.get(i));
            }
            return obj;
        } else if (args.size() == 1) {
            return args.get(0);
        } else {
            return null;
        }
    }

    /**
     * Merge maps right map override left map
     *
     * @param args
     * @return
     */
    static Map<String, Object> merge(Map<String, Object>... args) {
        if (args.length > 1) {
            Map<String, Object> obj = args[0];
            for (int i = 1; i < args.length; i++) {
                obj = mergeTwo(obj, args[i]);
            }
            return obj;
        } else if (args.length == 1) {
            return args[0];
        } else {
            return null;
        }
    }

    /**
     * @param obj1
     * @param obj2
     * @return
     */
    private static Map<String, Object> mergeTwo(Map<String, Object> obj1, Map<String, Object> obj2) {
        if (obj1 == null)
            obj1 = Objects.newHashMap();
        if (obj2 == null)
            return obj1;
        final Map<String, Object> dest_map = Objects.copy(obj1);
        final Map<String, Object> source_map = Objects.copy(obj2);

        return (Map<String, Object>) ObjectIterator.iterate(source_map, new Closure<ObjectIterator.IterateBean, Object>() {
            @Override
            public Object call(ObjectIterator.IterateBean i) {
                if (i.getPath().indexOf("[") != -1)
                    return i.getValue();
                Object o1 = dest_map;
                if (!Objects.isNullOrEmpty(i.getPath()))
                    o1 = ObjectPath.get(dest_map, i.getPath(), null);

                if (i.getValue() instanceof Map) {
                    if (o1 != null && o1 instanceof Map) {
                        //merge maps (only simple properties)
                        for (Map.Entry<String, Object> e : ((Map<String, Object>) o1).entrySet()) {
                            if (!((Map<String, Object>) i.getValue()).containsKey(e.getKey())) {
                                ((Map<String, Object>) i.getValue()).put(e.getKey(), e.getValue());
                            }
                        }
                    }
                    //o = o2 << o;
                } else if (i.getValue() instanceof List) {
                    //if(o1 && o1 instanceof List)
                    //o2 = o2 << o1;
                }
                return i.getValue();
            }
        });
    }

}
