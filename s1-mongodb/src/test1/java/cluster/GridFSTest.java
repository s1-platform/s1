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

package cluster;

import org.s1.cluster.dds.beans.Id;
import org.s1.cluster.dds.file.FileStorage;
import org.s1.misc.Closure;
import org.s1.misc.IOUtils;
import org.s1.table.errors.NotFoundException;
import org.s1.test.LoadTestUtils;
import org.s1.test.ServerTest;

import java.io.IOException;
import java.io.OutputStream;

/**
 * s1v2
 * User: GPykhov
 * Date: 23.01.14
 * Time: 12:16
 */
public class GridFSTest extends ServerTest {

    public void testFile(){

        int p = 10;
        title("File storage write-read-remove, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {

                FileStorage.FileWriteBean w = null;
                try{
                    w = FileStorage.createFileWriteBean(new Id(null,"test","aa"+input),
                            new FileStorage.FileMetaBean("test", "txt", "text/plain", 4, null));
                    try {
                        w.getOutputStream().write("qwer".getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    FileStorage.save(w);
                }finally {
                    FileStorage.closeAfterWrite(w);
                }

                FileStorage.FileReadBean r = null;
                try {
                    r = FileStorage.read(new Id(null,"test","aa"+input));

                    assertEquals("qwer", IOUtils.toString(r.getInputStream(), "UTF-8"));
                    assertEquals(4L, r.getMeta().getSize());
                    assertEquals("text/plain", r.getMeta().getContentType());
                    assertEquals("txt", r.getMeta().getExt());
                    assertEquals("test", r.getMeta().getName());
                    assertTrue(r.getMeta().getInfo().isEmpty());
                    assertNotNull(r.getMeta().getCreated());
                    assertNotNull(r.getMeta().getLastModified());

                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    FileStorage.closeAfterRead(r);
                }

                FileStorage.remove(new Id(null,"test","aa"+input));

                boolean b = false;
                r = null;
                try {
                    r = FileStorage.read(new Id(null,"test","aa"+input));
                } catch (NotFoundException e) {
                    b = true;
                } finally {
                    FileStorage.closeAfterRead(r);
                }

                assertTrue(b);

                return null;
            }
        }));

    }

}