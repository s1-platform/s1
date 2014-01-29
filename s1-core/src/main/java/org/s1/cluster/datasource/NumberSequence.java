package org.s1.cluster.datasource;

import com.hazelcast.core.IAtomicLong;
import org.s1.S1SystemError;
import org.s1.cluster.HazelcastWrapper;
import org.s1.cluster.Locks;
import org.s1.cluster.datasource.DistributedDataSource;
import org.s1.cluster.datasource.FileStorage;
import org.s1.cluster.node.ClusterNode;
import org.s1.misc.Closure;
import org.s1.misc.IOUtils;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
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
            return (Long) Locks.waitAndRun(NumberSequence.class.getName() + "#" + name, new Closure<String, Object>() {
                @Override
                public Object call(String input) {
                    IAtomicLong al = getAtomicLong(name);
                    long l = al.incrementAndGet();

                    //update node sequences
                    ClusterNode.call(NumberSequence.class, "set",
                            Objects.newHashMap(String.class, Object.class, "value", l, "name", name), name);
                    return l;
                }
            }, 10, TimeUnit.SECONDS);
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

    @Override
    public void runWriteCommand(String command, Map<String, Object> params) {
        if("set".equals(command)){
            String name = Objects.get(params,"name");
            long value = Objects.get(params,"value");
            getLocalStorage().write(name,value);
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

    private static Map<String,IAtomicLong> cache = new ConcurrentHashMap<String, IAtomicLong>();

    /**
     *
     * @param name
     * @return
     */
    private static IAtomicLong getAtomicLong(String name){
        if(!cache.containsKey(name)){
            //Get Atomic long and cache it
            IAtomicLong al = HazelcastWrapper.getInstance().getAtomicLong(NumberSequence.class.getName()+"#"+name);
            al.set(getLocalStorage().read(name));
            cache.put(name,al);
        }
        return cache.get(name);

    }
}
