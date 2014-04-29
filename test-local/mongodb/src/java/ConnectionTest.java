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

import com.mongodb.DB;
import org.s1.misc.Closure;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.options.Options;
import org.s1.testing.BasicTest;
import org.s1.testing.LoadTestUtils;
import org.testng.annotations.Test;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.02.14
 * Time: 12:42
 */
public class ConnectionTest extends BasicTest {

    @Test
    public void testDefault(){
        int p=10;

        final String n = Options.getStorage().get("MongoDB","connections.default.name");
        assertEquals(p, LoadTestUtils.run("test",p,p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input)  throws Exception {
                DB db = MongoDBConnectionHelper.getConnection(null);
                assertEquals(n,db.getName());
                db.getCollectionNames();
                if(input==0)
                    trace(db.getCollectionNames());
            }
        }));
    }

    @Test
    public void testAnother(){
        int p=10;

        final String n = Options.getStorage().get("MongoDB","connections.test.name");
        assertEquals(p, LoadTestUtils.run("test",p,p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input)  throws Exception {
                DB db = MongoDBConnectionHelper.getConnection("test");
                assertEquals(n,db.getName());
                db.getCollectionNames();
                if(input==0)
                    trace(db.getCollectionNames());
            }
        }));
    }

    @Test
    public void testError(){
        int p=10;

        assertEquals(p, LoadTestUtils.run("test",p,p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input)  throws Exception {
                MongoDBConnectionHelper.getConnection("test_not_found");
            }
        }));
    }

}
