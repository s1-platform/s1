package cluster;

import com.hazelcast.core.Hazelcast;
import com.mongodb.DBCollection;
import org.s1.S1SystemError;
import org.s1.cluster.datasource.FileStorage;
import org.s1.cluster.datasource.NumberSequence;
import org.s1.cluster.node.ClusterNode;
import org.s1.cluster.node.Transactions;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.mongodb.MongoDBDDS;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.s1.options.OptionsStorage;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;

/**
 * s1v2
 * User: GPykhov
 * Date: 20.01.14
 * Time: 20:57
 */
public class StressNode {

    public static void main(String[] args) throws Exception {
        String home = "";
        try {
            home = new File(StressNode.class.getResource("/").toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
        }
        System.setProperty("s1."+ OptionsStorage.CONFIG_HOME, home+ "/options");

        ClusterNode.start();
        try{
            final String collection = "stress1";
            DBCollection coll = MongoDBConnectionHelper.getConnection(null).getCollection(collection);
            System.out.println("count:>>>>>>>>>>>>>"+coll.count());
            if((coll.count()%5)!=0){
                throw new S1SystemError("something goes wrong!!!");
            }
            Transactions.run(new Closure<String, Object>() {
                @Override
                public Object call(String input) throws ClosureException {
                    for(int i=0;i<5;i++){
                        System.out.println(">>"+i);
                        MongoDBDDS.add(null,collection, ""+i,
                                Objects.newHashMap(String.class,Object.class,"name","test_"+i));
                        if(i==3){
                            //throw new RuntimeException("test");
                            //System.exit(-1);
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }

                    return null;
                }
            });
        }finally {
            ClusterNode.stop();
            Hazelcast.shutdownAll();
        }
    }

}
