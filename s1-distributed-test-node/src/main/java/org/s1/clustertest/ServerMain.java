package org.s1.clustertest;

import org.s1.test.TestAppServer;

import java.io.File;

/**
 * Main server class
 */
public class ServerMain extends TestAppServer {

    @Override
    protected int getPort() {
        return Integer.parseInt(PORT);
    }

    @Override
    protected String getAppPath() {
        return CONF+"/webapp";
    }

    @Override
    protected String getContext() {
        return "/s1";
    }

    @Override
    protected String getOptions() {
        return CONF+"/options";
    }

    private static final String CONF;
    private static final String PORT;

    static {
        PORT = System.getProperty("port","9000");
        String s = null;
        try{
            s = System.getProperty("conf",
                    new File(ServerMain.class.getResource("/").toURI()).getAbsolutePath()+"../../../distr/conf");
        }catch (Exception e){}
        CONF = s;
    }

    public static void main(String[] args) {
        ServerMain server = new ServerMain();
        server.run();
    }

}
