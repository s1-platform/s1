package cluster;

import org.s1.cluster.datasource.FileStorage;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.misc.FileUtils;
import org.s1.misc.IOUtils;
import org.s1.options.Options;
import org.s1.test.ClusterTest;
import org.s1.test.LoadTestUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * s1v2
 * User: GPykhov
 * Date: 23.01.14
 * Time: 12:16
 */
public class GridFSTest extends ClusterTest {

    public void testFile(){

        int p = 1;
        title("File storage write-read-remove, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {

                FileStorage.write("test", "aa" + input, new Closure<OutputStream, Boolean>() {
                    @Override
                    public Boolean call(OutputStream input) {
                        try {
                            input.write("qwer".getBytes());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    }
                }, new FileStorage.FileMetaBean("test", "txt", "text/plain", 4, null));

                try {
                    FileStorage.read("test", "aa" + input, new Closure<FileStorage.FileReadBean, Object>() {
                        @Override
                        public Object call(FileStorage.FileReadBean input) {
                            assertEquals("qwer", IOUtils.toString(input.getInputStream(), "UTF-8"));
                            assertEquals(4L, input.getMeta().getSize());
                            assertEquals("text/plain", input.getMeta().getContentType());
                            assertEquals("txt", input.getMeta().getExt());
                            assertEquals("test", input.getMeta().getName());
                            assertTrue(input.getMeta().getInfo().isEmpty());
                            assertNotNull(input.getMeta().getCreated());
                            assertNotNull(input.getMeta().getLastModified());
                            return null;
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                FileStorage.remove("test", "aa" + input);

                boolean b = false;
                try {
                    FileStorage.read("test", "aa" + input, new Closure<FileStorage.FileReadBean, Object>() {
                        @Override
                        public Object call(FileStorage.FileReadBean input) {

                            return null;
                        }
                    });
                } catch (NotFoundException e) {
                    b = true;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                assertTrue(b);

                return null;
            }
        }));

    }

}
