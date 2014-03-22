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

package cluster.dds;

import org.s1.cluster.HazelcastWrapper;
import org.s1.cluster.dds.DDSCluster;
import org.s1.cluster.dds.EntityIdBean;
import org.s1.cluster.dds.Transactions;
import org.s1.cluster.dds.sequence.NumberSequence;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.options.Options;
import org.s1.test.LoadTestUtils;
import org.s1.test.ServerTest;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * s1v2
 * User: GPykhov
 * Date: 22.03.14
 * Time: 21:56
 */
public class NumberSequenceTest extends ServerTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        File dir = new File(Options.getStorage().getSystem("numberSequence.home", System.getProperty("user.home") + File.separator + ".s1-sequences"));
        trace("NumberSequence.Home: " + dir.getAbsolutePath());
        boolean b = true;
        if(dir.exists()){
            for(File f:dir.listFiles()){
                b = b&&f.delete();
            }
        }
        trace("Clear: " + b);
    }

    public void testSequence(){
        int p = 10;
        title("Number sequence, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) throws ClosureException {

                for (long i = 0; i < 10L; i++) {
                    assertEquals(i, NumberSequence.next("qwe" + index));
                }

                return null;
            }
        }));
    }

    public void testTransactionSequence(){
        int p = 10;
        title("Number sequence in transaction, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) throws ClosureException {
                try{
                    DDSCluster.lockEntity(new EntityIdBean(NumberSequence.class,null,null,"transact"),new Closure<String, Object>() {
                        @Override
                        public Object call(String input) throws ClosureException {
                            Transactions.run(new Closure<String, Object>() {
                                @Override
                                public Object call(String input) throws ClosureException {
                                    NumberSequence.next("transact");
                                    NumberSequence.next("transact");
                                    return null;
                                }
                            });

                            return null;
                        }
                    },10, TimeUnit.SECONDS);
                }catch (TimeoutException e){
                    throw ClosureException.wrap(e);
                }
                return null;
            }
        }));

        assertEquals(p*2+1,NumberSequence.next("transact"));
    }

}