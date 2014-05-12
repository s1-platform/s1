package org.s1.cluster;

import com.hazelcast.core.IMap;
import org.s1.objects.Objects;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Grigory Pykhov
 */
public class LongRunningTasks {

    private static IMap<String,Long> tasks = null;

    private static IMap<String,Long> getTasks(){
        if(tasks ==null){
            synchronized (LongRunningTasks.class){
                if(tasks ==null){
                    tasks = HazelcastWrapper.getInstance().getMap("s1.longRunningTasks");
                }
            }
        }
        return tasks;
    }

    public static String start(){
        return start(null);
    }

    public static String start(String id){
        if(Objects.isNullOrEmpty(id))
            id = UUID.randomUUID().toString();
        getTasks().put(id,0L);
        return id;
    }

    public static void finish(String id){
        getTasks().remove(id);
    }

    public static void setProgress(String id, long progress){
        getTasks().put(id, progress);
    }

    public static void addProgress(String id, final long progress){
        String lid = Locks.lockQuite("longRunningTask:"+id,1, TimeUnit.SECONDS);
        try{
            long l = getTasks().get(id);
            getTasks().put(id,l+progress);
        }finally {
            Locks.releaseLock(lid);
        }
    }

    public static long getProgress(String id){
        if(!getTasks().containsKey(id))
            return -1L;
        return getTasks().get(id);
    }

}
