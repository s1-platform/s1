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

package weboperation;

import org.s1.S1SystemError;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.misc.IOUtils;
import org.s1.objects.Objects;
import org.s1.test.LoadTestUtils;
import org.s1.test.ServerTest;
import org.s1.test.TestHttpClient;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 10:45
 */
public class UploadTest extends ServerTest {

    public void testUploadDownload(){
        int p = 10;
        title("Upload download, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                String s = "qwerasdf"+input;
                String name = "name_"+input;
                String ct = "text/plain";
                try{
                    //required param
                    boolean b = true;
                    try{
                        client().getJSON(getContext() + "/dispatcher/Upload.download",
                                Objects.newHashMap(String.class, Object.class),null);
                        b = false;
                    }catch (RuntimeException e){
                        if(input==0)
                            trace(e.getMessage());
                    }
                    assertTrue(b);

                    //not found
                    TestHttpClient.HttpResponseBean r = client().get(getContext() + "/dispatcher/Upload.download",
                            Objects.newHashMap(String.class, Object.class, "id", "file"+input), null, null);
                    assertEquals(404, r.getStatus());

                    //upload
                    Map<String,Object> m = client().uploadFileForJSON(getContext() + "/dispatcher/Upload.upload",
                            new ByteArrayInputStream(s.getBytes()),name,ct,null);

                    String id = Objects.get(m,"id");

                    assertNotNull(id);

                    //download
                    r = client().get(getContext() + "/dispatcher/Upload.download",
                            Objects.newHashMap(String.class, Object.class, "id", id), null, null);
                    assertEquals(200,r.getStatus());
                    assertEquals(s, IOUtils.toString(r.getData(),"UTF-8"));
                    assertEquals(ct, r.getHeaders().get("Content-Type"));

                    //download for name
                    r = client().get(getContext() + "/dispatcher/Upload.downloadAsFile",
                            Objects.newHashMap(String.class, Object.class, "id", id), null, null);
                    assertEquals(200,r.getStatus());
                    assertEquals(s, IOUtils.toString(r.getData(),"UTF-8"));
                    assertEquals(ct, r.getHeaders().get("Content-Type"));
                    assertTrue(r.getHeaders().get("Content-Disposition").contains("attachment;filename=" + URLEncoder.encode(name,"UTF-8")));
                }catch (Exception e){
                    throw S1SystemError.wrap(e);
                }
                return null;
            }
        }));
    }

}
