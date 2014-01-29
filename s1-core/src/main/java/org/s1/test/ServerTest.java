package org.s1.test;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.s1.S1SystemError;
import org.s1.cluster.node.ClusterNode;
import org.s1.misc.FileUtils;
import org.s1.misc.IOUtils;
import org.s1.objects.Objects;

import java.io.*;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 12:00
 */
public abstract class ServerTest extends BasicTest {

    protected TestHttpClient client(){
        return new TestHttpClient("http","localhost",getPort());
    }

    protected int getPort(){
        return Objects.cast(getProperties().get("port"),Integer.class);
    }

    protected String getContext(){
        return Objects.cast(getProperties().get("context"),String.class);
    }

    protected String getWebXml(){
        String s = Objects.cast(getProperties().get("web.xml"),String.class);
        s = s.replace(".",File.separator);
        s = s+File.separator+"web.xml";
        return getTestClassesHome()+s;
    }

    private static boolean started = false;
    public static final String SERVER_HOME = System.getProperty("java.io.tmpdir")+File.separator+"s1-test-server";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if(!started){
            File tomcat_dir = new File(SERVER_HOME);
            tomcat_dir.mkdirs();
            FileUtils.deleteDir(tomcat_dir);

            //conf
            Tomcat tomcat = new Tomcat();
            tomcat.setBaseDir(SERVER_HOME);
            tomcat.enableNaming();
            tomcat.setHostname("localhost");
            tomcat.setPort(getPort());

            tomcat.getHost().setAppBase(SERVER_HOME+File.separator+"webapps");
            tomcat.getHost().setAutoDeploy(true);
            tomcat.getHost().setDeployOnStartup(true);

            File webinf = new File(SERVER_HOME+File.separator+"webapps"+getContext()+File.separator+"WEB-INF");
            webinf.mkdirs();
            FileUtils.copyFile(new File(getWebXml()), new File(webinf.getAbsolutePath() + File.separator + "web.xml"));
            Context ctx = tomcat.addWebapp(tomcat.getHost(), getContext(), SERVER_HOME+File.separator+"webapps"+getContext());
            trace("web.xml path: " + getWebXml());
            trace("Context path: " + getContext());
            try {
                tomcat.start();
            } catch (LifecycleException e) {
                throw S1SystemError.wrap(e);
            }
            trace("Server started: " + "localhost" + ":" + getPort());
            started = true;
        }
    }

}
