package org.s1.cluster.node;

import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Node local operation log.
 * If you want to provide custom implementation -
 * you should override this class and put its name to 'cluster.operationLogClass' options
 * <pre>
 * INFO - init
 * DEBUG - add,markDone
 * TRACE - add with params
 * </pre>
 */
public class NodeOperationLog {

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
                cl.callQuite(m);
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
                cl.callQuite(m);
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
