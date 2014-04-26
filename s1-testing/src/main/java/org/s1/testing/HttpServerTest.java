package org.s1.testing;

import org.s1.testing.httpclient.TestHttpClient;

/**
 * @author Grigory Pykhov
 */
public abstract class HttpServerTest extends BasicTest{

    protected int getPort(){
        return Integer.parseInt(properties.getProperty("http.port","9999"));
    }

    protected String getHost(){
        return properties.getProperty("http.host","localhost");
    }

    protected String getContext(){
        return properties.getProperty("http.context","");
    }

    protected TestHttpClient client(){
        return new TestHttpClient("http",getHost(),getPort());
    }

}
