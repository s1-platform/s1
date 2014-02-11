package org.s1.script;

/**
 * Memory heap
 */
public class MemoryHeap {

    private long limit;
    private long memory;

    public MemoryHeap(long memory) {
        this.memory = memory;
        this.limit = memory;
    }

    /**
     * Takes object size from heap
     *
     * @param obj
     * @throws ScriptLimitException
     */
    public synchronized void take(Object obj) throws ScriptLimitException{
        String s = ""+obj;
        take(s.length());
    }

    /**
     * Takes some bytes from heap
     *
     * @param bytes
     * @throws ScriptLimitException
     */
    public synchronized void take(long bytes) throws ScriptLimitException{
        memory-=bytes;
        if(memory<0)
            throw new ScriptLimitException(ScriptLimitException.Limits.MEMORY,limit);
    }

    public synchronized void release(long bytes){
        memory+=bytes;
    }
}
