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
