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
import org.s1.S1SystemError;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Distributed lock helper
 */
public class Locks {

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
        ILock lock = HazelcastWrapper.getInstance().getLock(lockId);
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
        }
    }

    /**
     * Run only if there is not other lock
     *
     * @param lockId
     * @param closure
     * @return
     */
    public static Object runIfFree(String lockId, Closure<String, Object> closure) throws ClosureException {
        try {
            return waitAndRun(lockId, closure, 0, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return null;
        }
    }

}
