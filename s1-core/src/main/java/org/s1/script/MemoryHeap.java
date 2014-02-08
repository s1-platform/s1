package org.s1.script;

import org.s1.objects.Objects;

import java.lang.instrument.Instrumentation;
import java.math.BigDecimal;
import java.util.Date;

/**
 * s1v2
 * User: GPykhov
 * Date: 08.02.14
 * Time: 18:09
 */
public class MemoryHeap {

    private long limit;
    private long memory;

    public MemoryHeap(long memory) {
        this.memory = memory;
        this.limit = memory;
    }

    public synchronized void take(Object obj){
        String s = ""+obj;
        take(s.length());
    }

    public synchronized void take(long bytes){
        memory-=bytes;
        System.err.println(memory);
        if(memory<0)
            throw new MemoryLimitException("Limit: "+limit);
    }

    public synchronized void release(long bytes){
        memory+=bytes;
    }
}
