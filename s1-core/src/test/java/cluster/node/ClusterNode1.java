package cluster.node;

import org.s1.cluster.datasource.NumberSequence;
import org.s1.cluster.datasource.FileStorage;
import org.s1.cluster.node.ClusterNode;
import org.s1.misc.Closure;
import org.s1.options.Options;
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
public class ClusterNode1 {

    public static void main(String[] args) throws Exception {
        String home = "";
        try {
            home = new File(ClusterNode1.class.getResource("/").toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
        }
        System.setProperty("s1."+ OptionsStorage.CONFIG_HOME, home+ "/options1");

        //clear sequences
        File dir = new File(Options.getStorage().getSystem("numberSequence.home", System.getProperty("user.home") + File.separator + ".s1-sequences"));
        boolean b = true;
        if(dir.exists()){
            for(File f:dir.listFiles()){
                b = b&&f.delete();
            }
        }
        System.out.println("Clear: "+dir.getAbsolutePath()+":"+b);

        ClusterNode.start();

        FileStorage.remove("test", "a1");

        FileStorage.write("test", "a1", new Closure<OutputStream, Boolean>() {
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

        //sequence
        System.out.println("0>>>>>>>>>>>>>"+NumberSequence.next("test"));
        System.out.println("1>>>>>>>>>>>>>"+NumberSequence.next("test"));
        System.out.println("2>>>>>>>>>>>>>"+NumberSequence.next("test"));

        System.out.println("0>>>>>>>>>>>>>"+NumberSequence.next("test1"));
        System.out.println("1>>>>>>>>>>>>>"+NumberSequence.next("test1"));

        System.out.println(">>>>>>>>>>>>> Now CliterNode2 must be started");

        Thread.sleep(30000);
        System.out.println("4>>>>>>>>>>>>>"+NumberSequence.next("test"));
        System.out.println("5>>>>>>>>>>>>>"+NumberSequence.next("test"));
    }

}
