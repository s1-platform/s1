package cluster;

import org.s1.cluster.Session;
import org.s1.misc.Closure;
import org.s1.options.OptionsStorage;

import java.io.File;
import java.net.URISyntaxException;

/**
 * s1v2
 * User: GPykhov
 * Date: 20.01.14
 * Time: 20:57
 */
public class SessionNode1 {

    public static void main(String[] args) throws Exception {
        String home = "";
        try {
            home = new File(SessionNode1.class.getResource("/").toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
        }
        System.setProperty("s1."+ OptionsStorage.CONFIG_HOME, home+ "/options1");

        try{
            Session.start("test1");
            if(!Session.getSessionBean().getUserId().equals(Session.ANONYMOUS))
                throw new RuntimeException("error");
            if(Session.getSessionBean().get("a")!=null)
                throw new RuntimeException("error");

            Session.getSessionBean().setUserId("user1");
            Session.getSessionBean().set("a", "aaa");

            if(!Session.getSessionBean().getUserId().equals("user1"))
                throw new RuntimeException("error");
            if(!Session.getSessionBean().get("a").equals("aaa"))
                throw new RuntimeException("error");
        }finally {
            Session.end("test1");
        }

        System.out.println(">>>>>>>>>>>>> Now SessionNode2 must be started");
        Thread.sleep(10000);

        try{
            Session.start("test1");
            System.out.println(Session.getSessionBean().getUserId());
            if (!Session.getSessionBean().getUserId().equals("user1"))
                throw new RuntimeException("error");
            if (!Session.getSessionBean().get("a").equals("aaa"))
                throw new RuntimeException("error");
        }finally {
            Session.end("test1");
        }

    }

}
