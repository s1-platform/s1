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

package org.s1.test;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.s1.S1SystemError;
import org.s1.misc.FileUtils;
import org.s1.options.OptionsStorage;

import java.io.File;
import java.net.URISyntaxException;

/**
 * Test app server
 */
public abstract class TestAppServer implements Runnable{

    public static final String SERVER_HOME = System.getProperty("java.io.tmpdir")+"/s1-test-server";

    protected abstract int getPort();
    protected abstract String getAppPath();
    protected abstract String getContext();
    protected abstract String getOptions();

    protected String getClassesHome(){
        try {
            return new File(this.getClass().getResource("/").toURI()).getAbsolutePath()+File.separator;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public void run() {
        //set options path
        String opt = getOptions();
        System.setProperty("s1."+ OptionsStorage.CONFIG_HOME, opt);
        System.err.println("TestAppServer: s1." + OptionsStorage.CONFIG_HOME + "=" + opt);


        String host = "localhost";
        int port = getPort();
        String path = getAppPath();
        String contextPath = getContext();
        if(!contextPath.startsWith("/"))
            contextPath = "/"+contextPath;

        File tomcat_dir = new File(SERVER_HOME);
        tomcat_dir.mkdirs();
        FileUtils.deleteDir(tomcat_dir);

        //conf
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(SERVER_HOME);
        tomcat.enableNaming();
        tomcat.setHostname(host);
        tomcat.setPort(port);

        tomcat.getHost().setAppBase(SERVER_HOME +File.separator+"webapps");
        tomcat.getHost().setAutoDeploy(true);
        tomcat.getHost().setDeployOnStartup(true);

        Context ctx = tomcat.addWebapp(tomcat.getHost(), contextPath, path);
        System.err.println("TestAppServer: Webapp path: "+path);
        System.err.println("TestAppServer: Context path: "+contextPath);
        try {
            tomcat.start();
        } catch (LifecycleException e) {
            throw S1SystemError.wrap(e);
        }
        System.err.println("TestAppServer: Server started: "+host+":"+port);
        tomcat.getServer().await();
    }

}
