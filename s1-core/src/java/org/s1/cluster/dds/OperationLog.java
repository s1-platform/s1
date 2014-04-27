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

package org.s1.cluster.dds;

import org.s1.cluster.dds.beans.MessageBean;
import org.s1.misc.Closure;
import org.s1.objects.Objects;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Node local operation log.
 * You should provide custom implementation -
 * you should override this class and put its name to 'cluster.operationLogClass' options.
 * <b>Do not use this class in production - its log is in-memory.</b>
 * <pre>
 * INFO - init
 * DEBUG - add,markDone
 * TRACE - add with params
 * </pre>
 */
public class OperationLog {

    /**
     *
     */
    public void initialize(){

    }

    private SortedMap<Long,MessageBean> map = new TreeMap<Long, MessageBean>();
    private Map<Long,Boolean> doneMap = Objects.newHashMap();

    /**
     *
     * @param id
     * @param cl
     */
    public synchronized void listFrom(long id, Closure<MessageBean,Object> cl){
        for(MessageBean m:map.values()){
            if(m.getId()>id){
                cl.call(m);
            }
        }
    }

    /**
     *
     * @param cl
     */
    public synchronized void listUndone(Closure<MessageBean,Object> cl){
        for(MessageBean m:map.values()){
            if(!doneMap.get(m.getId())){
                cl.call(m);
            }
        }
    }

    /**
     *
     * @param m
     */
    public synchronized void addToLocalLog(MessageBean m){
        map.put(m.getId(),m);
        doneMap.put(m.getId(),false);
    }

    /**
     *
     * @param id
     */
    public synchronized void markDone(final long id){
        doneMap.put(id,true);
    }

    /**
     *
     * @return
     */
    public synchronized long getLastId(){
        return map.size()==0?0L:map.lastKey();
    }

}
