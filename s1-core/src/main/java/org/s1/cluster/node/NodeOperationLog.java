package org.s1.cluster.node;

import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * INFO - init
 * DEBUG - add,markDone
 * TRACE - add with params
 */
public class NodeOperationLog {

    public void initialize(){

    }

    private SortedMap<Long,MessageBean> map = new TreeMap<Long, MessageBean>();
    private Map<Long,Boolean> doneMap = Objects.newHashMap();

    public synchronized void listFrom(long id, Closure<MessageBean,Object> cl){
        for(MessageBean m:map.values()){
            if(m.getId()>id){
                cl.callQuite(m);
            }
        }
    }

    public synchronized void listUndone(Closure<MessageBean,Object> cl){
        for(MessageBean m:map.values()){
            if(!doneMap.get(m.getId())){
                cl.callQuite(m);
            }
        }
    }

    public synchronized void addToLocalLog(MessageBean m){
        map.put(m.getId(),m);
        doneMap.put(m.getId(),false);
    }

    public synchronized void markDone(final long id){
        doneMap.put(id,true);
    }

    public synchronized long getLastId(){
        return map.size()==0?0L:map.lastKey();
    }

}
