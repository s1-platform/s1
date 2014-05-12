package org.s1.cluster;

import com.hazelcast.core.IMap;
import org.s1.objects.Objects;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

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
        getTasks().computeIfPresent(id,new BiFunction<String, Long, Long>() {
            @Override
            public Long apply(String s, Long aLong) {
                return progress+aLong;
            }
        });
    }

    public static long getProgress(String id){
        if(!getTasks().containsKey(id))
            return -1L;
        return getTasks().get(id);
    }

}
