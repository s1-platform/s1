package org.s1.clustertest;

import com.mongodb.BasicDBObject;
import org.s1.cluster.Locks;
import org.s1.cluster.node.Transactions;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.mongodb.MongoDBDDS;
import org.s1.mongodb.MongoDBQueryHelper;
import org.s1.mongodb.cluster.MongoDBOperationLog;
import org.s1.objects.Objects;
import org.s1.table.TablesFactory;
import org.s1.weboperation.MapWebOperation;
import org.s1.weboperation.WebOperationMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Test operation
 */
public class TestOperation extends MapWebOperation{

    @WebOperationMethod
    public Map<String, Object> clear(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        MongoDBConnectionHelper.getConnection(null).getCollection(TablesFactory.getTable("accounts").getCollection()).remove(new BasicDBObject());
        MongoDBConnectionHelper.getConnection(null).getCollection(TablesFactory.getTable("operations").getCollection()).remove(new BasicDBObject());
        return Objects.newHashMap();
    }

    @WebOperationMethod
    public Map<String, Object> pay(Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        final String src = Objects.get(params,"src");
        final String dest = Objects.get(params,"dest");
        final double sum = Objects.get(Double.class,params,"sum",1.0D);

        final String accounts = TablesFactory.getTable("accounts").getCollection();
        final String operations = TablesFactory.getTable("operations").getCollection();

        Transactions.run(new Closure<String, Object>() {
            @Override
            public Object call(String input) throws ClosureException {
                try{
                    TablesFactory.getTable("accounts").changeState(src,"plusBalance",Objects.newHashMap(String.class,Object.class,
                            "sum",-sum
                    ),null);
                    TablesFactory.getTable("accounts").changeState(dest,"plusBalance",Objects.newHashMap(String.class,Object.class,
                            "sum",sum
                    ),null);
                    /*final Map<String,Object> src_acc = MongoDBQueryHelper.get(null,accounts,new BasicDBObject("id",src));
                    final Map<String,Object> dst_acc = MongoDBQueryHelper.get(null,accounts,new BasicDBObject("id",dest));
                    double src_balance = Objects.get(Double.class,src_acc,"balance",0.0D);
                    double dst_balance = Objects.get(Double.class,dst_acc,"balance",0.0D);
                    if(sum>0 && sum>src_balance){
                        throw new Exception("Not enough balance");
                    }
                    if(sum<0 && sum>dst_balance){
                        throw new Exception("Not enough balance");
                    }
                    Objects.set(src_acc,"balance",src_balance-sum);
                    Objects.set(dst_acc,"balance",dst_balance+sum);
                    //save

                    Locks.waitAndRun("pay:" + src, new Closure<String, Object>() {
                        @Override
                        public Object call(String input) throws ClosureException {
                            MongoDBDDS.set(null, accounts, new BasicDBObject("id", src), src_acc);
                            return null;
                        }
                    },30,TimeUnit.SECONDS);
                    Locks.waitAndRun("pay:" + dest, new Closure<String, Object>() {
                        @Override
                        public Object call(String input) throws ClosureException {
                            MongoDBDDS.set(null, accounts, new BasicDBObject("id", src), src_acc);
                            return null;
                        }
                    },30,TimeUnit.SECONDS);
                    MongoDBDDS.set(null,accounts,new BasicDBObject("id",dest),dst_acc);*/
                    //add operation
                    TablesFactory.getTable("operations").changeState(null,"add",Objects.newHashMap(String.class,Object.class,
                            "src",Objects.newHashMap("id",src),
                            "dest",Objects.newHashMap("id",dest),
                            "sum",sum
                            ),null);
                }catch (Throwable e){
                    throw ClosureException.wrap(e);
                }
                return null;
            }
        });
        return Objects.newHashMap();
    }

    @Override
    protected Map<String, Object> process(String method, Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return processClassMethods(this,method,params,request,response);
    }
}
