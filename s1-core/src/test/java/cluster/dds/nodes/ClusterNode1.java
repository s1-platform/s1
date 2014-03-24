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

package cluster.dds.nodes;

import org.s1.S1SystemError;
import org.s1.cluster.ClusterLifecycleAction;
import org.s1.cluster.Locks;
import org.s1.cluster.dds.beans.Id;
import org.s1.cluster.dds.beans.StorageId;
import org.s1.cluster.dds.Transactions;
import org.s1.cluster.dds.sequence.NumberSequence;
import org.s1.cluster.dds.file.FileStorage;

import org.s1.options.Options;
import org.s1.options.OptionsStorage;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

/**
 * s1v2
 * User: GPykhov
 * Date: 20.01.14
 * Time: 20:57
 */
public class ClusterNode1 {

    public static void main(String[] args) throws Exception {
        String home = "";
        try {
            home = new File(ClusterNode1.class.getResource("/").toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
        }
        System.setProperty("s1."+ OptionsStorage.CONFIG_HOME, home+ "/options1");

        //clear sequences
        File dir = new File(Options.getStorage().getSystem("numberSequence.home", System.getProperty("user.home") + File.separator + ".s1-sequences"));
        boolean b = true;
        if(dir.exists()){
            for(File f:dir.listFiles()){
                b = b&&f.delete();
            }
        }
        System.out.println("Clear: "+dir.getAbsolutePath()+":"+b);
        new ClusterLifecycleAction().start();

        FileStorage.remove(new Id(null,"test","a1"));

        FileStorage.FileWriteBean fb = null;
        try{
            fb = FileStorage.createFileWriteBean(new Id(null,"test","a1"), new FileStorage.FileMetaBean("aaa", "txt", "text/plain", 4, null));
            try {
                fb.getOutputStream().write("qwer".getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            FileStorage.save(fb);
        }finally {
            FileStorage.closeAfterWrite(fb);
        }

        System.out.println("file a1 writed");

        //sequence
        System.out.println("0>>>>>>>>>>>>>"+NumberSequence.next("test"));
        System.out.println("1>>>>>>>>>>>>>"+NumberSequence.next("test"));
        System.out.println("2>>>>>>>>>>>>>"+NumberSequence.next("test"));

        System.out.println("0>>>>>>>>>>>>>"+NumberSequence.next("test1"));
        System.out.println("1>>>>>>>>>>>>>"+NumberSequence.next("test1"));

        System.out.println(">>>>>>>>>>>>> Now CliterNode2 must be started");

        Thread.sleep(30000);
        System.out.println("4>>>>>>>>>>>>>"+NumberSequence.next("test"));
        System.out.println("5>>>>>>>>>>>>>"+NumberSequence.next("test"));

        while(true){
            String lockId = null;
            String id = null;
            try{
                lockId = Locks.lockEntityQuite(new StorageId(NumberSequence.class, null, null, "transact"), 30, TimeUnit.SECONDS);
                id = Transactions.begin();

                long l = NumberSequence.next("transact");
                System.out.println(">>>>>>>>>>>>>" + l);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw S1SystemError.wrap(e);
                }
                NumberSequence.set("transact",l+1);

                Transactions.commit(id);
            }catch (Throwable e){
                Transactions.rollbackOnError(id, e);
                throw new RuntimeException(e.getMessage(),e);
            }finally {
                Locks.releaseLock(lockId);
            }

            Thread.sleep(2000);
        }
    }

}
