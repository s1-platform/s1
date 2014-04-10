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

package user;

import org.s1.cluster.Session;
import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.s1.test.BasicTest;
import org.s1.test.LoadTestUtils;
import org.s1.test.ServerTest;
import org.s1.test.TestHttpClient;
import org.s1.user.UserBean;
import org.s1.user.Users;

import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 10:45
 */
public class UsersTest extends BasicTest {

    public void testGet(){
        int p = 10;
        title("Get user, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input)  {

                UserBean m = Users.getUser(Session.ANONYMOUS);
                assertEquals(Session.ANONYMOUS,m.getId());

                return null;
            }
        }));
    }

}
