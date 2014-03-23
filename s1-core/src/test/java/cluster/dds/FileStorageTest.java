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
            public Object call(Integer input)  {

                FileStorage.FileWriteBean fw = null;
                try{
                    fw = FileStorage.createFileWriteBean("test","aa"+input,new FileStorage.FileMetaBean("test", "txt", "text/plain", 4, null));
                    try {
                        fw.getOutputStream().write("qwer".getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    FileStorage.save(fw);
                }finally {
                    FileStorage.closeAfterWrite(fw);
                }

                try {
                    FileStorage.FileReadBean fr = null;
                    try{
                        fr = FileStorage.read("test","aa"+input);
                        assertEquals("qwer", IOUtils.toString(fr.getInputStream(), "UTF-8"));
                        assertEquals(4L, fr.getMeta().getSize());
                        assertEquals("text/plain", fr.getMeta().getContentType());
                        assertEquals("txt", fr.getMeta().getExt());
                        assertEquals("test", fr.getMeta().getName());
                        assertTrue(fr.getMeta().getInfo().isEmpty());
                        assertNotNull(fr.getMeta().getCreated());
                        assertNotNull(fr.getMeta().getLastModified());

                    }finally {
                        FileStorage.closeAfterRead(fr);
                    }

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
