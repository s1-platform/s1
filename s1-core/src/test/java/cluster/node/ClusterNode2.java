package cluster.node;

import org.s1.cluster.datasource.NumberSequence;
import org.s1.cluster.datasource.FileStorage;
import org.s1.cluster.node.ClusterNode;
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

        ClusterNode.start();

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
