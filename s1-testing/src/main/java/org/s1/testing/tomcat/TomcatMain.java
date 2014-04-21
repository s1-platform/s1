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

package org.s1.testing.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

/**
 * @author Grigory Pykhov
 */
public class TomcatMain {

    public static final String SERVER_HOME = System.getProperty("java.io.tmpdir")+"/s1-test-server";

    public static void main(String[] args) throws IOException {
        if((args.length!=4 && args.length!=7) ||
                (!args[0].equals("start") && !args[0].equals("stop"))){
            throw new IllegalArgumentException("Usage:\n" +
                    "start: TomcatMain start <host> <port> <context> <webapp-path> <stop-port> <stop-key>\n" +
                    "stop: TomcatMain stop <host> <stop-port> <stop-key>");
        }
        String cmd = args[0];
        if(cmd.equals("start")){
            String host = args[1];
            int port = Integer.parseInt(args[2]);
            String context = args[3];
            String path = args[4];
            int shutdownport = Integer.parseInt(args[5]);
            String key = args[6];
            start(host,port,context,path,shutdownport,key);
        }else{
            String host = args[1];
            int port = Integer.parseInt(args[2]);
            String key = args[3];
            stop(host, port, key);
        }
    }

    private static void start(String host, int port, String context, String path, int stopPort, String stopKey){
        if(!context.startsWith("/"))
            context = "/"+context;

        File tomcat_dir = new File(SERVER_HOME);
        tomcat_dir.mkdirs();

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(SERVER_HOME);
        tomcat.enableNaming();
        tomcat.setHostname(host);
        tomcat.setPort(port);
        tomcat.getServer().setPort(stopPort);
        tomcat.getServer().setShutdown(stopKey);

        tomcat.getHost().setAppBase(SERVER_HOME + File.separator+"webapps");
        tomcat.getHost().setAutoDeploy(true);
        tomcat.getHost().setDeployOnStartup(true);

        Context ctx = tomcat.addWebapp(tomcat.getHost(), context, path);
        System.err.println("TestAppServer: Webapp path: "+path);
        System.err.println("TestAppServer: Context path: "+context);
        try {
            tomcat.start();
        } catch (LifecycleException e) {
            throw new RuntimeException(e.getMessage(),e);
        }
        System.err.println("TestAppServer: Server started: "+host+":"+port);
        tomcat.getServer().await();
    }

    private static void stop(String host, int stopPort, String stopKey) throws IOException {
        Socket clientSocket = new Socket(host, stopPort);
        clientSocket.getOutputStream().write(stopKey.getBytes());
        clientSocket.getOutputStream().close();
        clientSocket.close();
    }

}
