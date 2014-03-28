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
import org.s1.objects.Objects;

import java.io.File;

/**
 * Base test class for server tests
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

    protected String getWebapp(){
        String s = Objects.cast(getProperties().get("webapp"),String.class);
        s = s.replace(".",File.separator);
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

            Context ctx = tomcat.addWebapp(tomcat.getHost(), getContext(), getWebapp());
            trace("webapp path: " + getWebapp());
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
