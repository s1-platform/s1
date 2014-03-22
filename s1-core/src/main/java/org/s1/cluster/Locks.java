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

import com.hazelcast.core.IList;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import org.s1.S1SystemError;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Distributed lock helper
 */
public class Locks {

    private static IMap<String,Long> locks = HazelcastWrapper.getInstance().getMap("s1.locks");
    private static ILock lock = HazelcastWrapper.getInstance().getLock("s1.locks");
    private static ThreadLocal<List<String>> localLocks = new ThreadLocal<List<String>>();

    /**
     *
     * @param lockId
     * @param closure
     * @param timeout
     * @param tu
     * @return
     * @throws TimeoutException
     */
    public static Object waitAndRun(String lockId, Closure<String, Object> closure, long timeout, TimeUnit tu) throws TimeoutException, ClosureException {
        return waitAndRun(Objects.newArrayList(lockId),closure,timeout,tu);
    }

    /**
     *
     * @param lockIds
     * @param closure
     * @param timeout
     * @param tu
     * @return
     * @throws TimeoutException
     * @throws ClosureException
     */
    public static Object waitAndRun(List<String> lockIds, Closure<String, Object> closure, long timeout, TimeUnit tu) throws TimeoutException, ClosureException {
        if(localLocks.get() == null)
            localLocks.set(Objects.newArrayList(String.class));

        List<String> nonLocalIds = Objects.newArrayList();

        long t = System.currentTimeMillis();
        Object ret = null;
        while(true){
            boolean b = false;
            try{
                /* BEGIN: lock to check */
                try{
                    boolean lb = lock.tryLock(timeout,tu);
                    if(!lb)
                        throw new TimeoutException("Timeout waiting lock");

                    for(String new_lock:lockIds){
                        for(String existing_lock:locks.keySet()){
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
                        for(String new_lock:lockIds){
                            if(locks.containsKey(new_lock)){

                            }else{
                                locks.put(new_lock,System.currentTimeMillis());
                                localLocks.get().add(new_lock);
                                nonLocalIds.add(new_lock);
                            }
                        }
                    }
                } catch (InterruptedException e){
                    throw S1SystemError.wrap(e);
                } finally {
                    try{
                        lock.unlock();
                    }catch (Throwable e){}
                }
                /* END: lock to check */

                //run code
                if(!b){
                    ret = closure.call(null);
                    break;
                }

            }finally {
                //remove locks
                if(!b){
                    for(String new_lock:nonLocalIds){
                        locks.remove(new_lock);
                        localLocks.get().remove(new_lock);
                    }
                }
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
        return ret;
        /*ILock lock = HazelcastWrapper.getInstance().getLock("s1.locks");
        boolean b = false;
        try {
            b = lock.tryLock(timeout, tu);
        } catch (Exception e) {
            throw S1SystemError.wrap(e);
        }
        if (b) {
            try {
                return closure.call(lockId);
            } finally {
                lock.unlock();
            }
        } else {
            throw new TimeoutException("Lock timeout occurs: " + lockId);
        }*/
    }

}
