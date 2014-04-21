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

package cluster;

import com.hazelcast.core.Hazelcast;
import com.mongodb.DBCollection;
import org.s1.S1SystemError;
import org.s1.cluster.ClusterLifecycleAction;
import org.s1.cluster.dds.Transactions;
import org.s1.cluster.dds.beans.Id;
import org.s1.misc.Closure;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.mongodb.cluster.MongoDBDDS;
import org.s1.objects.Objects;
import org.s1.options.OptionsStorage;

import java.io.File;
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

        new ClusterLifecycleAction().start();

        try{
            final String collection = "stress1";
            DBCollection coll = MongoDBConnectionHelper.getConnection(null).getCollection(collection);
            System.out.println("count:>>>>>>>>>>>>>"+coll.count());
            if((coll.count()%5)!=0){
                throw new S1SystemError("something goes wrong!!!");
            }

            String id = null;
            try{
                id = Transactions.begin();

                for(int i=0;i<5;i++){
                    System.out.println(">>"+i);
                    MongoDBDDS.add(new Id(null,collection,""+i),
                            Objects.newHashMap(String.class, Object.class, "name", "test_" + i));
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

                Transactions.commit(id);
            }catch (Throwable e){
                Transactions.rollbackOnError(id,e);
            }finally {

            }
        }finally {
            new ClusterLifecycleAction().stop();
            Hazelcast.shutdownAll();
        }
    }

}
