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

package pages;

import org.s1.misc.Closure;
import org.s1.misc.IOUtils;
import org.s1.testing.HttpServerTest;
import org.s1.testing.LoadTestUtils;
import org.s1.testing.httpclient.TestHttpClient;
import org.testng.annotations.Test;


/**
 * s1v2
 * User: GPykhov
 * Date: 28.03.2014
 * Time: 21:53
 */
public class PagesTest extends HttpServerTest {

    @Test
    public void testPages(){
        client().get(getContext() + "/pages/page1", null, null);
        client().get(getContext() + "/pages/page2", null, null);
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test",p,p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {
                TestHttpClient.HttpResponseBean r = null;

                r = client().get(getContext() + "/pages/page1", null, null);
                assertEquals(200,r.getStatus());
                assertTrue(IOUtils.toString(r.getData(),"UTF-8").contains("<h1>aaa1</h1>"));

                r = client().get(getContext() + "/pages/page2", null, null);
                assertEquals(200,r.getStatus());
                assertTrue(IOUtils.toString(r.getData(),"UTF-8").contains("<h1>aaa2</h1>"));

                r = client().get(getContext() + "/pages/page1/", null, null);
                assertEquals(404,r.getStatus());

                r = client().get(getContext() + "/pages/page3", null, null);
                assertEquals(404,r.getStatus());

            }
        }));
    }

}
