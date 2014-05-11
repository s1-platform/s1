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

import org.s1.misc.IOUtils;
import org.s1.objects.Objects;
import org.s1.testing.HttpServerTest;
import org.s1.testing.LoadTestUtils;
import org.s1.testing.httpclient.TestHttpClient;
import org.testng.annotations.Test;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.util.Map;
import java.util.UUID;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 10:45
 */
public class UploadTest extends HttpServerTest {

    @Test
    public void testUploadDownload(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test",p,p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {
                String s = "qwerasdf"+input;
                String name = "name_"+input;
                String ct = "text/plain";
                try{
                    //required param
                    boolean b = true;
                    try{
                        client().getJSON(getContext() + "/dispatcher/Upload.download",
                                Objects.newHashMap(String.class, Object.class));
                        b = false;
                    }catch (RuntimeException e){
                        if(input==0)
                            trace(e.getMessage());
                    }
                    assertTrue(b);

                    //not found
                    TestHttpClient.HttpResponseBean r = client().get(getContext() + "/dispatcher/Upload.download",
                            Objects.newHashMap(String.class, Object.class, "id", "file"+input), null);
                    assertEquals(404, r.getStatus());

                    //upload
                    Map<String,Object> m = client().uploadFileForJSON(getContext() + "/dispatcher/Upload.upload",
                            new ByteArrayInputStream(s.getBytes()),name,ct);

                    String id = Objects.get(m,"id");

                    assertNotNull(id);

                    //download
                    r = client().get(getContext() + "/dispatcher/Upload.download",
                            Objects.newHashMap(String.class, Object.class, "id", id), null);
                    assertEquals(200,r.getStatus());
                    assertEquals(s, IOUtils.toString(r.getData(),"UTF-8"));
                    assertEquals(ct, r.getHeaders().get("Content-Type"));

                    //download for name
                    r = client().get(getContext() + "/dispatcher/Upload.downloadAsFile",
                            Objects.newHashMap(String.class, Object.class, "id", id), null);
                    assertEquals(200,r.getStatus());
                    assertEquals(s, IOUtils.toString(r.getData(),"UTF-8"));
                    assertEquals(ct, r.getHeaders().get("Content-Type"));
                    assertTrue(r.getHeaders().get("Content-Disposition").contains("attachment;filename=" + URLEncoder.encode(name,"UTF-8")));
                }catch (Exception e){
                    throw S1SystemError.wrap(e);
                }
            }
        }));
    }

    @Test
    public void testUploadAsync(){
        int p = 2;
        final String id = "123";
        assertEquals("upload/"+id,Objects.get(client().postJSON(getContext() + "/dispatcher/Monitor.startTask", Objects.newSOHashMap("id", "upload/"+id)),"result"));

        StringBuilder sb = new StringBuilder("qwerasdf");
        for(long i=0;i<100000;i++){
            sb.append("a");
        }
        final String s = sb.toString();
        final String name = "name_1";
        final String ct = "text/plain";

        assertEquals(p, LoadTestUtils.run("test",p,p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int input) throws Exception {


                try{
                    Map<String,Object> m = null;
                    //upload
                    if(input==1) {
                        trace("uploading...");
                        m = client().uploadFileForJSON(getContext() + "/dispatcher/Upload.upload?id="+id,
                                new ByteArrayInputStream(s.getBytes()), name, ct);
                        trace("uploaded");
                    }else {
                        trace("waiting...");

                        //progress
                        long progress = 0;
                        while(progress!=-1) {
                            m = client().postJSON(getContext() + "/dispatcher/Monitor.getProgress", Objects.newSOHashMap("id", "upload/" + id));
                            progress = Objects.get(Long.class, m, "result");
                            //if (input == 0)
                                trace(progress);
                            sleep(10);
                        }

                        //download
                        TestHttpClient.HttpResponseBean r = client().get(getContext() + "/dispatcher/Upload.download",
                                Objects.newHashMap(String.class, Object.class, "id", id), null);
                        assertEquals(200, r.getStatus());
                        //assertEquals(s, IOUtils.toString(r.getData(),"UTF-8"));
                        assertEquals(s.length(), IOUtils.toString(r.getData(), "UTF-8").length());
                        assertEquals(ct, r.getHeaders().get("Content-Type"));
                    }
                }catch (Exception e){
                    throw S1SystemError.wrap(e);
                }
            }
        }));
    }

}
