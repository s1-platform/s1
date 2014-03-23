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

import org.s1.objects.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Transactions wrapper for distributed data source commands
 */
public class Transactions {
    private static final Logger LOG = LoggerFactory.getLogger(Transactions.class);

    private static ThreadLocal<String> local = new ThreadLocal<String>();
    private static final Map<String,LogBean> transactionLog = new ConcurrentHashMap<String, LogBean>();

    /**
     * Check if you`re in transaction
     * @return
     */
    public static boolean isInTransaction(){
        return local.get()!=null;
    }

    /**
     *
     */
    static synchronized void clear(){
        local = new ThreadLocal<String>();
        transactionLog.clear();
        LOG.info("Transaction log is clear");
    }

    /**
     *
     * @return
     */
    public static String begin() {
        if(local.get()!=null){
            return null;
        }else{
            String id = UUID.randomUUID().toString();
            transactionLog.put(id, new LogBean());
            local.set(id);
            return id;
        }
    }

    /**
     *
     * @param id
     */
    public static void commit(String id){
        if(Objects.isNullOrEmpty(id))
            return;
        List<CommandBean> l = Objects.newArrayList();
        for(CommandBean c:transactionLog.get(id).getList()){
            l.add(c);
        }

        local.remove();
        transactionLog.remove(id);

        DDSCluster.call(new MessageBean(null,null,null,null,null,
                Objects.newHashMap(String.class, Object.class, "list", l)));
    }

    /**
     *
     * @param id
     */
    public static void rollbackOnError(String id, Throwable e){
        if(Objects.isNullOrEmpty(id)) {
            throw new RollbackException(e.getMessage(),e);
        }
        local.remove();
        transactionLog.remove(id);
    }

    /**
     * @param b
     */
    static void addOperation(CommandBean b){
        String id = local.get();
        transactionLog.get(id).getList().add(b);
    }

    /**
     *
     */
    private static class LogBean{
        private List<CommandBean> list = Objects.newArrayList();

        public List<CommandBean> getList() {
            return list;
        }
    }

}
