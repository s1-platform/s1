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

package org.s1.objects;

import org.s1.S1SystemError;
import org.s1.misc.Closure;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Object helper
 * <p>This class contains static methods to work with {@link java.util.Map} containing another maps or lists ({@link java.util.List}) or just simple types.</p>
 */
public class Objects {

    /**
     * @param date
     * @param format
     * @return
     */
    public static Date parseDate(String date, String format) {
        try {
            return new SimpleDateFormat(format).parse(date);
        } catch (ParseException e) {
            throw S1SystemError.wrap(e);
        }
    }

    /**
     * @param date
     * @param format
     * @return
     */
    public static String formatDate(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }

    /**
     * Create map from array (must contains even elements count name1,value1,name2,value2...)
     * if contains odd elements - last will be null
     *
     * @param args
     * @return
     */
    public static <K, V> Map<K, V> newHashMap(Object... args) {
        Map<K, V> m = new HashMap<K, V>();
        for (int i = 0; i < args.length; i += 2) {
            m.put((K) args[i], i + 1 >= args.length ? null : (V) args[i + 1]);
        }
        return m;
    }

    /**
     * @param k
     * @param v
     * @param args
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> Map<K, V> newHashMap(Class<K> k, Class<V> v, Object... args) {
        Map<K, V> m = new HashMap<K, V>();
        for (int i = 0; i < args.length; i += 2) {
            m.put((K) args[i], i + 1 >= args.length ? null : (V) args[i + 1]);
        }
        return m;
    }

    /**
     * @param args
     * @param <T>
     * @return
     */
    public static <T> List<T> newArrayList(T... args) {
        List<T> l = new ArrayList<T>();
        for (int i = 0; i < args.length; i++) {
            l.add(args[i]);
        }
        return l;
    }

    /**
     * @param t
     * @param args
     * @param <T>
     * @return
     */
    public static <T> List<T> newArrayList(Class<T> t, T... args) {
        List<T> l = new ArrayList<T>();
        for (int i = 0; i < args.length; i++) {
            l.add(args[i]);
        }
        return l;
    }

    /**
     * @param c
     * @param cl
     * @param <T>
     * @return
     */
    public static <T> T find(Collection<T> c, Closure<T, Boolean> cl) {
        for (T el : c) {
            if (cl.callQuite(el)) {
                return el;
            }
        }
        return null;
    }

    /**
     * @param c
     * @param cl
     * @param <T>
     * @return
     */
    public static <T> List<T> findAll(Collection<T> c, Closure<T, Boolean> cl) {
        List<T> l = new ArrayList<T>();
        for (T el : c) {
            if (cl.callQuite(el)) {
                l.add(el);
            }
        }
        return l;
    }

    /**
     * @param c
     * @param cl
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> Map.Entry<K, V> find(Map<K, V> c, Closure<Map.Entry<K, V>, Boolean> cl) {
        return find(c.entrySet(), cl);
    }

    /**
     * @param c
     * @param cl
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> List<Map.Entry<K, V>> findAll(Map<K, V> c, Closure<Map.Entry<K, V>, Boolean> cl) {
        return findAll(c.entrySet(), cl);
    }

    /**
     * Copy object deeply
     *
     * @param orig
     * @return
     */
    public static <T> T copy(T orig) {
        if (orig == null)
            return null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(orig);
            oos.flush();
            ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bin);
            return (T) ois.readObject();
        } catch (Exception e) {
            throw S1SystemError.wrap(e);
        }
    }

    /**
     * Returns true if object is null or empty
     *
     * @param obj
     * @return
     */
    public static boolean isNullOrEmpty(Object obj) {
        if (obj == null)
            return true;
        if (obj instanceof String) {
            return (((String) obj).isEmpty());
        }
        if (obj instanceof Map) {
            return ((Map) obj).isEmpty();
        }
        if (obj instanceof List) {
            return ((List) obj).isEmpty();
        }
        if (obj instanceof Set) {
            return ((Set) obj).isEmpty();
        }
        return false;
    }

    /**
     * Returns true if objects are equals by value
     *
     * @param o1
     * @param o2
     * @return
     */
    public static boolean equals(Object o1, Object o2) {
        if (o1 == o2)
            return true;
        if (o1 == null && o2 == null)
            return true;
        if (o1 == null || o2 == null)
            return false;
        //number
        if (!o1.getClass().equals(o2.getClass()) && (o1 instanceof Number || o2 instanceof Number)) {
            try {
                BigDecimal b1 = cast(o1, BigDecimal.class);
                BigDecimal b2 = cast(o2, BigDecimal.class);
                return b1.compareTo(b2)==0;
            } catch (Throwable e) {
                return false;
            }
        }
        //map
        if (o1 instanceof Map && o2 instanceof Map) {
            return diff((Map<String, Object>) o1, (Map<String, Object>) o2).size() == 0;
        } else if (o1 instanceof List && o2 instanceof List) {
            Map<String, Object> m1 = newHashMap("list", o1);
            Map<String, Object> m2 = newHashMap("list", o2);
            return diff(m1, m2).size() == 0;
        } else {
            return o1.equals(o2);
        }
    }


    /**
     * Resolves type and returns casted object
     *
     * @param obj
     * @param type
     * @return
     */
    public static <T> T cast(Object obj, String type) {
        return (T) cast(obj, resolveType(type));
    }

    public static <T> T cast(Object obj, Class<T> type) {
        return ObjectType.cast(obj, type);
    }

    public static Class resolveType(String type) {
        return ObjectType.resolveType(type);
    }

    public static void set(Map<String, Object> data, String path, Object val) {
        ObjectPath.set(data, path, val);
    }

    public static <T> T get(Class<T> cl, Map<String, Object> data, String path) {
        return get(cl, data, path, null);
    }

    public static <T> T get(Class<T> cl, Map<String, Object> data, String path, T def) {
        return cast(get(data, path, def), cl);
    }

    public static <T> T get(Map<String, Object> data, String path) {
        return get(data, path, null);
    }

    public static <T> T get(Map<String, Object> data, String path, T def) {
        return ObjectPath.get(data, path, def);
    }

    public static Map<String, Object> merge(Map<String, Object>... args) {
        return ObjectMerge.merge(args);
    }

    public static Map<String, Object> merge(List<Map<String, Object>> args) {
        return ObjectMerge.merge(args);
    }

    public static List<ObjectDiff.DiffBean> diff(Map<String, Object> oldObject, Map<String, Object> newObject) {
        return ObjectDiff.diff(oldObject, newObject);
    }

    public static Map<String, Object> iterate(Map<String, Object> data, Closure<ObjectIterator.IterateBean, Object> cl) {
        return ObjectIterator.iterate(data, cl);
    }

    public static Map<String, Object> toWire(Map<String, Object> data) {
        return ObjectWire.toWire(data);
    }

    public static Map<String, Object> fromWire(Map<String, Object> data) {
        return ObjectWire.fromWire(data);
    }
}
