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

package org.s1.cluster;

import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import org.s1.S1SystemError;
import org.s1.cluster.dds.DDSCluster;
import org.s1.cluster.dds.beans.StorageId;
import org.s1.cluster.dds.Transactions;
import org.s1.objects.Objects;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Distributed lock helper
 */
public class Locks {

    private static IMap<String,Long> locks = null;
    private static ILock lock = null;
    private static ThreadLocal<List<String>> localLocks = new ThreadLocal<List<String>>();
    private static Map<String,List<String>> runningLocks = new ConcurrentHashMap<String, List<String>>();

    private static IMap<String,Long> getLocks(){
        if(locks==null){
            synchronized (Locks.class){
                if(locks==null){
                    locks = HazelcastWrapper.getInstance().getMap("s1.locks");
                }
            }
        }
        return locks;
    }

    private static ILock getLock(){
        if(lock==null){
            synchronized (Locks.class){
                if(lock==null){
                    lock = HazelcastWrapper.getInstance().getLock("s1.locks");
                }
            }
        }
        return lock;
    }

    public static void destroy(){
        locks = null;
        lock = null;
    }

    /**
     *
     * @param lockId
     * @param timeout
     * @param tu
     * @return
     * @throws TimeoutException
     */
    public static String lock(String lockId, long timeout, TimeUnit tu) throws TimeoutException {
        return lock(Objects.newArrayList(lockId),timeout,tu);
    }

    /**
     *
     * @param lockId
     * @param timeout
     * @param tu
     * @return
     */
    public static String lockQuite(String lockId, long timeout, TimeUnit tu) {
        return lockQuite(Objects.newArrayList(lockId), timeout, tu);
    }

    /**
     *
     * @param lockIds
     * @param timeout
     * @param tu
     * @return
     */
    public static String lockQuite(List<String> lockIds, long timeout, TimeUnit tu) {
        try{
            return lock(lockIds,timeout,tu);
        }catch (TimeoutException e){
            throw S1SystemError.wrap(e);
        }
    }

    /**
     *
     * @param lockIds
     * @param timeout
     * @param tu
     * @return
     * @throws TimeoutException
     */
    public static String lock(List<String> lockIds, long timeout, TimeUnit tu) throws TimeoutException {
        String id = UUID.randomUUID().toString();

        if(localLocks.get() == null)
            localLocks.set(Objects.newArrayList(String.class));

        long t = System.currentTimeMillis();
        Object ret = null;
        while(true){
            boolean b = false;
            /* BEGIN: lock to check */
            try{
                boolean lb = getLock().tryLock(timeout,tu);
                if(!lb)
                    throw new TimeoutException("Timeout waiting lock");

                for(String new_lock:lockIds){
                    for(String existing_lock:getLocks().keySet()){
                        if(new_lock.startsWith(existing_lock) ||
                                existing_lock.startsWith(new_lock)){
                            if(!localLocks.get().contains(existing_lock)){
                                b = true;
                                break;
                            }
                        }
                    }
                    if(b)
                        break;
                }
                if(!b){
                    //set locks
                    runningLocks.put(id,Objects.newArrayList(String.class));
                    for(String new_lock:lockIds){
                        if(getLocks().containsKey(new_lock)){

                        }else{
                            getLocks().put(new_lock,System.currentTimeMillis());
                            localLocks.get().add(new_lock);
                            runningLocks.get(id).add(new_lock);
                        }
                    }
                }
            } catch (InterruptedException e){
                throw S1SystemError.wrap(e);
            } finally {
                try{
                    getLock().unlock();
                }catch (Throwable e){}
            }
            /* END: lock to check */

            //run code
            if(!b){
                break;
            }

            if(System.currentTimeMillis()-t>tu.toMillis(timeout)){
                throw new TimeoutException("Timeout waiting lock: "+lockIds);
            }
            try{
                Thread.sleep(1);
            }catch (InterruptedException e){
                throw S1SystemError.wrap(e);
            }
        }
        return id;
    }

    /**
     *
     * @param id
     */
    public static void releaseLock(String id){
        if(Objects.isNullOrEmpty(id))
            return;
        if(runningLocks.containsKey(id)) {
            for (String new_lock : runningLocks.get(id)) {
                getLocks().remove(new_lock);
                localLocks.get().remove(new_lock);
            }
        }
    }

    /**
     *
     * @param e
     * @param timeout
     * @param tu
     * @return
     */
    public static String lockEntityQuite(StorageId e, long timeout, TimeUnit tu) {
        return lockEntitiesQuite(Objects.newArrayList(e), timeout, tu);
    }

    /**
     *
     * @param e
     * @param timeout
     * @param tu
     * @return
     * @throws TimeoutException
     */
    public static String lockEntity(StorageId e, long timeout, TimeUnit tu) throws TimeoutException {
        return lockEntities(Objects.newArrayList(e), timeout, tu);
    }

    /**
     *
     * @param e
     * @param timeout
     * @param tu
     * @return
     */
    public static String lockEntitiesQuite(final List<StorageId> e, long timeout, TimeUnit tu) {
        try{
            return lockEntities(e,timeout,tu);
        }catch (TimeoutException ex){
            throw S1SystemError.wrap(ex);
        }
    }

    /**
     *
     * @param e
     * @param timeout
     * @param tu
     * @return
     * @throws TimeoutException
     */
    public static String lockEntities(final List<StorageId> e, long timeout, TimeUnit tu) throws TimeoutException {
        if(Transactions.isInTransaction()){
            return null;
        }else{
            List<String> l = Objects.newArrayList();
            for(StorageId _e:e){
                l.add(_e.getLockName());
            }
            String id = Locks.lock(l,timeout,tu);
            for(StorageId _e:e){
                DDSCluster.flush(_e);
            }
            return id;
        }
    }

}
