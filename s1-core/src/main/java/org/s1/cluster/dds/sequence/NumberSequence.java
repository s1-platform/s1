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

package org.s1.cluster.dds.sequence;

import com.hazelcast.core.IAtomicLong;
import org.s1.S1SystemError;
import org.s1.cluster.HazelcastWrapper;
import org.s1.cluster.Locks;
import org.s1.cluster.dds.*;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Distributed number sequence
 */
public class NumberSequence extends DistributedDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(NumberSequence.class);

    /**
     * Get next number from sequence, if sequence is empty, 0L will be returned and sequence will be created
     *
     * @param name
     * @return
     */
    public static long next(final String name){
        try{
            long r = (Long)DDSCluster.lockEntity(new EntityIdBean(NumberSequence.class,null,null,name),new Closure<String, Object>() {
                @Override
                public Object call(String input) throws ClosureException {
                    long l = getLocalStorage().read(name);
                    l++;
                    DDSCluster.call(new MessageBean(NumberSequence.class, null, null, name, "set",
                            Objects.newHashMap(String.class, Object.class, "value", l)));
                    return l;
                }
            }, 10, TimeUnit.SECONDS);
            return r;
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

    @Override
    public void runWriteCommand(CommandBean cmd) {
        if("set".equals(cmd.getCommand())){
            long value = Objects.get(cmd.getParams(), "value");
            getLocalStorage().write(cmd.getEntity(),value);
        }
    }

    private static NumberSequenceLocalStorage localStorage;

    private static synchronized NumberSequenceLocalStorage getLocalStorage(){
        if(localStorage==null){
            String cls = Options.getStorage().getSystem("numberSequence.localStorageClass",NumberSequenceLocalStorage.class.getName());
            try{
                localStorage = (NumberSequenceLocalStorage)Class.forName(cls).newInstance();
            }catch (Exception e){
                LOG.warn("Cannot initialize NumberSequenceLocalStorage ("+cls+"): "+e.getMessage(),e);
            }
        }
        return localStorage;
    }
}
