package org.s1.testing;

import org.s1.testing.httpclient.TestHttpClient;

/**
 * @author Grigory Pykhov
 */
public abstract class HttpServerTest extends BasicTest{

    protected int getPort(){
        String s = System.getProperty("s1test.http.port",properties.getProperty("http.port","9999"));
        return Integer.parseInt(s);
    }

    protected String getHost(){
        String s = System.getProperty("s1test.http.host",properties.getProperty("http.host","localhost"));
        return s;
    }

    protected String getContext(){
        String s = System.getProperty("s1test.http.context",properties.getProperty("http.context",""));
        return s;
    }

    protected TestHttpClient client(){
        return new TestHttpClient("http",getHost(),getPort());
    }

}
