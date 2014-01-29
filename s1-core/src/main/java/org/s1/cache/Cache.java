package org.s1.cache;

import org.s1.S1SystemError;
import org.s1.misc.Closure;
import org.s1.objects.Objects;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Cache
 */
public class Cache {

    private int capacity;
    private long ttl = 0;
    private TimeUnit tu;

    private Map<String,Object> cache = new ConcurrentHashMap<String,Object>();

    private Map<String,Long> gets = new ConcurrentHashMap<String, Long>();

    private Map<String,Long> created = new ConcurrentHashMap<String, Long>();

    private Map<String,Object> locks = new HashMap<String,Object>();
    private static final String LOCK_ALL = "__ALL_LOCK__";

    public Cache(int capacity){
        this.capacity = capacity;
    }

    public Cache(int capacity, long ttl, TimeUnit tu){
        this.capacity = capacity;
        this.ttl = ttl;
        this.tu = tu;
    }

    public Map<String,Object> getCache(){
        return Objects.copy(cache);
    }

    public Map<String,Long> getGetsStat(){
        return Objects.copy(gets);
    }

    protected void lockAll(){
        while(true){
            synchronized (locks){
                if(locks.size()==0){
                    locks.put(LOCK_ALL,new Object());
                    break;
                }
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw S1SystemError.wrap(e);
            }
        }
    }

    protected void unlockAll(){
        synchronized (locks){
            locks.remove(LOCK_ALL);
        }
    }

    protected void lock(String name){
        while(true){
            synchronized (locks){
                if(!locks.containsKey(name) && !locks.containsKey(LOCK_ALL)){
                    locks.put(name,new Object());
                    break;
                }
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw S1SystemError.wrap(e);
            }
        }
    }

    protected void unlock(String name){
        synchronized (locks){
            locks.remove(name);
        }
    }

    protected void checkSize(){
        lockAll();
        try{
            if(cache.size()<=capacity)
                return;
            Map<String,Double> weights = new HashMap<String, Double>();
            long createdMin = Long.MAX_VALUE;
            long createdMax = 0;
            long getsMin = Long.MAX_VALUE;
            long getsMax = 0;
            for(String key:cache.keySet()){
                long g = gets.get(key);
                long cr = created.get(key);
                if(g>getsMax)
                    getsMax = g;
                if(g<getsMin)
                    getsMin = g;
                if(cr>createdMax)
                    createdMax = cr;
                if(cr<createdMin)
                    createdMin = cr;
            }
            LinkedList<String> sorted = new LinkedList<String>();
            LinkedList<Double> sortedWeights = new LinkedList<Double>();
            for(String key:cache.keySet()){
                long g = gets.get(key);
                long cr = created.get(key);
                Double weight = getWeight(cr,g,createdMax,createdMin,getsMax,getsMin);
                //if("test0".equals(key))
                    //System.out.println(g+":"+weight);
                weights.put(key, weight);
                //if("test0".equals(key))
                //    System.out.println(weight);
                int pos = 0;
                for(Double w:sortedWeights){
                    if(weight<w)
                        break;
                    pos++;
                }
                sortedWeights.add(pos,weight);
                sorted.add(pos,key);
            }
            //System.out.println("---------------");
            //System.out.println(sorted);
            //System.out.println(sortedWeights);

            //check size
            while(cache.size()>(capacity-(int)(capacity*0.2D))){
                String f = sorted.getFirst();
                sorted.removeFirst();
                gets.remove(f);
                created.remove(f);
                cache.remove(f);
            }
        }finally {
            unlockAll();
        }
    }

    protected double getWeight(long created, long gets, long createdMax, long createdMin, long getsMax, long getsMin){
        double weightGets = 0.8D;
        double weightCreated = 0.2D;
        double crw = 0D;
        if(createdMax!=createdMin)
            crw = 1.0D - ((createdMax-created)*1.0D/(createdMax-createdMin)*1.0D);
        double gw = 0D;
        if(getsMax!=getsMin)
            gw = 1.0D - ((getsMax-gets)*1.0D/(getsMax-getsMin)*1.0D);
        return weightGets*gw+weightCreated*crw;
    }

    /**
     *
     * @param name
     * @param closure
     * @param <T>
     * @return
     */
    public <T> T get(String name, Closure<String,T> closure){
        T obj = null;
        lock(name);
        try {
            obj = (T)cache.get(name);
            if(obj!=null){
                if(ttl>0 && (created.get(name)+tu.toMillis(ttl)<System.currentTimeMillis())){
                    //invalidate
                    obj = null;
                }
            }
            if(obj!=null){
                gets.put(name, gets.get(name) + 1);
                return obj;
            }
        } finally {
            unlock(name);
        }

        //add obj
        obj = closure.callQuite(name);

        lock(name);
        try {

            created.put(name,System.currentTimeMillis());
            gets.put(name, 1L);
            cache.put(name, obj);

        } finally {
            unlock(name);
        }
        checkSize();
        return obj;
    }

    /**
     *
     * @param cls
     * @param name
     * @param closure
     * @param <T>
     * @return
     */
    public <T> T get(Class<T> cls, String name, Closure<String,T> closure){
        return Objects.cast(get(name,closure),cls);
    }

    public void invalidate(String name){
        lock(name);
        try{
            cache.remove(name);
            created.remove(name);
            gets.remove(name);
        }finally {
            unlock(name);
        }
    }

    public void invalidateAll(){
        lockAll();
        try{
            cache.clear();
            created.clear();
            gets.clear();
        }finally {
            unlockAll();
        }
    }

}
