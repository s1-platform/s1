package log;/*
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

import com.mongodb.DBCollection;
import org.s1.mongodb.log.MongoDBLogStorage;
import org.s1.objects.Objects;
import org.s1.testing.HttpServerTest;
import org.s1.testing.LoadTestUtils;
import org.s1.testing.httpclient.TestHttpClient;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 13.03.14
 * Time: 17:13
 */
public class LogStorageTest extends HttpServerTest {

    @BeforeMethod
    protected void clear() throws Exception {
        DBCollection coll = MongoDBLogStorage.getCollection();
        coll.drop();
        trace("log4j collection cleared");
        TestHttpClient client = client();
        //authenticate
        client.postJSON(getContext() + "/dispatcher/User.login", Objects.newHashMap(
                String.class, Object.class,
                "name", "root",
                "password", "root"
        ));
        //set log level
        client.postJSON(getContext() + "/dispatcher/Monitor.setLogLevel", Objects.newSOHashMap(
                "nodeId", "node-1",
                "level", "DEBUG"
        ));
    }

    @AfterMethod
    protected void setLogLevelBack(){
        TestHttpClient client = client();
        //authenticate
        client.postJSON(getContext() + "/dispatcher/User.login", Objects.newHashMap(
                String.class, Object.class,
                "name", "root",
                "password", "root"
        ));
        //set log level
        client.postJSON(getContext() + "/dispatcher/Monitor.setLogLevel", Objects.newSOHashMap(
                "nodeId", "node-1",
                "level", "INFO"
        ));
    }

    @Test
    public void testLog(){
        int p = 10;
        title("Log, parallel: " + p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input)  throws Exception {
                TestHttpClient client = client();
                //authenticate
                client.postJSON(getContext() + "/dispatcher/User.login", Objects.newHashMap(
                        String.class, Object.class,
                        "name", "root",
                        "password", "root"
                ));

                //get
                Map<String, Object> m = client.postJSON(getContext() + "/dispatcher/Monitor.listNodeLogs", Objects.newHashMap(
                        String.class, Object.class,
                        "nodeId", "node-1"
                        //,"search",Objects.newHashMap("name","")
                ));

                assertTrue(Objects.get(Long.class, m, "count") > 0);
                assertTrue(Objects.get(List.class, m, "list").size() > 0);
                assertNotNull(Objects.get(m, "list[0].name"));
                if (input == 0)
                    trace(Objects.get(m, "list[0]"));

            }
        }));
    }

}
