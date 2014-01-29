package weboperation;

import org.apache.http.HttpResponse;
import org.s1.S1SystemError;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.misc.IOUtils;
import org.s1.objects.Objects;
import org.s1.test.LoadTestUtils;
import org.s1.test.ServerTest;

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
        int p = 100;
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
                    HttpResponse r = client().get(getContext() + "/dispatcher/Upload.download",
                            Objects.newHashMap(String.class, Object.class, "id", "file"+input), null, null);
                    assertEquals(404, r.getStatusLine().getStatusCode());

                    //upload
                    Map<String,Object> m = client().uploadFileForJSON(getContext() + "/dispatcher/Upload.upload",
                            new ByteArrayInputStream(s.getBytes()),name,ct,null);

                    String id = Objects.get(m,"id");

                    assertNotNull(id);

                    //download
                    r = client().get(getContext() + "/dispatcher/Upload.download",
                            Objects.newHashMap(String.class, Object.class, "id", id), null, null);
                    assertEquals(200,r.getStatusLine().getStatusCode());
                    assertEquals(s, IOUtils.toString(r.getEntity().getContent(),"UTF-8"));
                    assertEquals(ct, r.getEntity().getContentType().getValue());

                    //download for name
                    r = client().get(getContext() + "/dispatcher/Upload.downloadAsFile",
                            Objects.newHashMap(String.class, Object.class, "id", id), null, null);
                    assertEquals(200,r.getStatusLine().getStatusCode());
                    assertEquals(s, IOUtils.toString(r.getEntity().getContent(),"UTF-8"));
                    assertEquals(ct, r.getEntity().getContentType().getValue());
                    assertEquals("attachment;filename=" + URLEncoder.encode(name,"UTF-8"), r.getLastHeader("Content-Disposition").getValue());
                }catch (Exception e){
                    throw S1SystemError.wrap(e);
                }
                return null;
            }
        }));
    }

}
