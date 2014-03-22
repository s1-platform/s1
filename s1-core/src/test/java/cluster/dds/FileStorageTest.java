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

import org.s1.cluster.dds.file.FileStorage;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.misc.FileUtils;
import org.s1.misc.IOUtils;
import org.s1.options.Options;
import org.s1.test.LoadTestUtils;
import org.s1.test.ServerTest;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * s1v2
 * User: GPykhov
 * Date: 22.03.14
 * Time: 21:55
 */
public class FileStorageTest extends ServerTest {

    public void testFile(){

        //clear
        FileUtils.deleteDir(new File(Options.getStorage().getSystem(String.class, "fileStorage.home")));

        int p = 10;
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

                assertTrue(new File(Options.getStorage().getSystem("fileStorage.home") + File.separator + "test" + File.separator + "aa" + input).exists());
                assertTrue(new File(Options.getStorage().getSystem("fileStorage.home") + File.separator + "test" + File.separator + "aa" + input + ".json").exists());

                FileStorage.remove("test", "aa" + input);

                if (input == 0)
                    trace(new File(Options.getStorage().getSystem("fileStorage.home") + File.separator + "test" + File.separator + "aa" + input).getAbsolutePath());

                assertTrue(!new File(Options.getStorage().getSystem("fileStorage.home") + File.separator + "test" + File.separator + "aa" + input).exists());
                assertTrue(!new File(Options.getStorage().getSystem("fileStorage.home") + File.separator + "test" + File.separator + "aa" + input + ".json").exists());

                return null;
            }
        }));

    }

}
