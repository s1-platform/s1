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

package cluster.dds.nodes;

import org.s1.cluster.ClusterLifecycleAction;
import org.s1.cluster.dds.sequence.NumberSequence;
import org.s1.cluster.dds.file.FileStorage;
import org.s1.misc.Closure;
import org.s1.options.OptionsStorage;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;

/**
 * s1v2
 * User: GPykhov
 * Date: 20.01.14
 * Time: 20:57
 */
public class ClusterNode2 {

    public static void main(String[] args) throws Exception {
        String home = "";
        try {
            home = new File(ClusterNode2.class.getResource("/").toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
        }
        System.setProperty("s1."+ OptionsStorage.CONFIG_HOME, home+ "/options2");

        new ClusterLifecycleAction().start();

        System.out.println("3>>>>>>>>>>>>>"+NumberSequence.next("test"));
        System.out.println("2>>>>>>>>>>>>>"+NumberSequence.next("test1"));

        Thread.sleep(20000);

        FileStorage.remove("test", "a2");

        FileStorage.write("test", "a2", new Closure<OutputStream, Boolean>() {
            @Override
            public Boolean call(OutputStream input) {
                try {
                    input.write("qwer".getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
        }, new FileStorage.FileMetaBean("aaa", "txt", "text/plain", 4, null));

    }

}
