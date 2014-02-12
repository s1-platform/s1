package org.s1.cluster.node;

import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
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

    public static final String LIST_GROUP = "list";
    public static final String LIST_COMMAND = "list";

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
     * All operation calls will be combined in one atomic transaction
     *
     * @param closure
     * @return
     */
    public static <T> T run(Closure<String,T> closure) throws ClosureException{
        if(local.get()!=null){
            return closure.call(local.get());
        }else{
            String id = UUID.randomUUID().toString();
            beginTransaction(id);
            local.set(id);
            try{
                T o = closure.call(id);
                local.remove();
                commit(id);
                return o;
            } catch (Throwable t){
                local.remove();
                rollback(id);
                throw ClosureException.wrap(ClosureException.getCause(t));
            } finally {
            }
        }
    }

    /**
     *
     * @param id
     */
    private static void beginTransaction(String id){
        transactionLog.put(id, new LogBean());
    }

    /**
     *
     * @param id
     */
    private static void commit(String id){
        ClusterNode.call(null,LIST_COMMAND,
                Objects.newHashMap(String.class,Object.class,"list",transactionLog.get(id).getList()),
                LIST_GROUP);
        transactionLog.remove(id);
    }

    /**
     *
     * @param id
     */
    private static void rollback(String id){
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
