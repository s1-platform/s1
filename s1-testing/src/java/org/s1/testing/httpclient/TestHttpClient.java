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

package org.s1.testing.httpclient;

import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.s1.format.json.JSONFormat;
import org.s1.format.json.JSONFormatException;
import org.s1.misc.IOUtils;
import org.s1.objects.Objects;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Http client for server testing
 */
public class TestHttpClient {

    /**
     *
     * @param schema
     * @param host
     * @param port
     */
    public TestHttpClient(String schema, String host, int port){
        //System.setProperty("javax.net.debug", "ssl");
        this.client = new DefaultHttpClient();
        this.context = new BasicHttpContext();
        this.host = new HttpHost(host,port,schema);

        CookieStore cookieStore = new BasicCookieStore();
        context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    protected HttpClient client;
    protected HttpContext context;
    protected HttpHost host;

    public HttpClient getHttpClient(){
        return client;
    }

    public HttpContext getHttpContext(){
        return context;
    }

    public HttpHost getHttpHost(){
        return host;
    }

    private void printIfError(String url, HttpResponseBean r){
        if(!(""+r.getStatus()).startsWith("2")){
            System.out.println("Http status ("+url+"): "+r.getStatus()+"\n" +
                    "Response:\n"+
                    new String (r.getData(),Charset.forName("UTF-8"))
            );
        }
    }

    /**
     *
     * @param u
     * @param data
     * @return
     */
    protected String getURL(String u, Map<String,Object> data){
        if(data==null)
            data = new HashMap<String,Object>();

        String query = "";
        int i=0;
        for(String key:data.keySet()){
            if(i!=0)
                query+="&";
            try {
                query+=(key+"="+ URLEncoder.encode("" + data.get(key), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e.getMessage(),e);
            }
            i++;
        }
        if(!query.isEmpty())
            u+=("?"+query);
        return u;
    }

    /**
     *
     * @param u
     * @param data
     * @param headers
     * @return
     */
    public HttpResponseBean get(String u, Map<String,Object> data, Map<String,String> headers){
        if(headers==null)
            headers = new HashMap<String,String>();
        u = getURL(u,data);
        HttpGet get = new HttpGet(u);
        try{
            for(String h:headers.keySet()){
                get.setHeader(h,headers.get(h));
            }

            //HttpPost post = new HttpPost(LOGIN_URL);
            client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Test Browser");
            client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
            //client.getParams().setParameter(ClientPNames.COOKIE_POLICY, org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY);

            HttpResponse resp = null;
            try {
                resp = client.execute(host,get,context);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(),e);
            }
            Map<String,String> rh = new HashMap<String,String>();
            for(Header h:resp.getAllHeaders()){
                rh.put(h.getName(),h.getValue());
            }
            try {
                HttpResponseBean r = new HttpResponseBean(resp.getStatusLine().getStatusCode(),rh,EntityUtils.toByteArray(resp.getEntity()));
                printIfError(u,r);
                return r;
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(),e);
            }
        }finally {
            get.releaseConnection();
        }
    }

    /**
     *
     * @param u
     * @param data
     * @param headers
     * @return
     */
    public HttpResponseBean postForm(String u, Map<String,Object> data, Map<String,String> headers){
        if(headers==null)
            headers = new HashMap<String,String>();

        u = getURL(u,null);
        HttpPost post = new HttpPost(u);
        try{
            headers.put("Content-Type","application/x-www-form-urlencoded");
            for(String h:headers.keySet()){
                post.setHeader(h,""+headers.get(h));
            }
            //HttpPost post = new HttpPost(LOGIN_URL);
            List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
            if(data!=null){
                for(String h:data.keySet()){
                    params.add(new BasicNameValuePair(h,(String)data.get(h)));
                }
            }
            UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(params, Charset.forName("UTF-8"));
            post.setEntity(urlEncodedFormEntity);
            client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Test Browser");
            client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
            //client.getParams().setParameter(ClientPNames.COOKIE_POLICY, org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY);

            HttpResponse resp = null;
            try {
                resp = client.execute(host,post,context);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(),e);
            }
            Map<String,String> rh = new HashMap<String,String>();
            for(Header h:resp.getAllHeaders()){
                rh.put(h.getName(),h.getValue());
            }
            try {
                HttpResponseBean r = new HttpResponseBean(resp.getStatusLine().getStatusCode(),rh,EntityUtils.toByteArray(resp.getEntity()));
                printIfError(u,r);
                return r;
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(),e);
            }
        }finally {
            post.releaseConnection();
        }
    }

    /**
     *
     * @param u
     * @param data
     * @param headers
     * @return
     */
    public HttpResponseBean post(String u, InputStream data, Map<String,String> headers){
        if(headers==null)
            headers = new HashMap<String,String>();

        u = getURL(u,null);
        HttpPost post = new HttpPost(u);
        try{
            for(String h:headers.keySet()){
                post.setHeader(h,headers.get(h));
            }
            HttpEntity request = new InputStreamEntity(data,-1);
            post.setEntity(request);
            client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Test Browser");
            client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
            //client.getParams().setParameter(ClientPNames.COOKIE_POLICY, org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY);

            HttpResponse resp = null;
            try {
                resp = client.execute(host,post,context);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(),e);
            }
            Map<String,String> rh = new HashMap<String,String>();
            for(Header h:resp.getAllHeaders()){
                rh.put(h.getName(),h.getValue());
            }
            try {
                HttpResponseBean r = new HttpResponseBean(resp.getStatusLine().getStatusCode(),rh,EntityUtils.toByteArray(resp.getEntity()));
                printIfError(u,r);
                return r;
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(),e);
            }
        }finally {
            post.releaseConnection();
        }
    }

    /**
     *
     * @param u
     * @param data
     * @param name
     * @param contentType
     * @return
     */
    public HttpResponseBean uploadFile(String u, InputStream data, String name, String contentType){
        Map<String,Object> headers = new HashMap<String,Object>();

        u = getURL(u,null);
        HttpPost post = new HttpPost(u);
        try{
            MultipartEntity request = new MultipartEntity();
            ContentBody body = new InputStreamBody(data,contentType,name);
            request.addPart("file", body);
            post.setEntity(request);
            for(String h:headers.keySet()){
                post.setHeader(h,""+headers.get(h));
            }

            client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Test Browser");
            client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
            //client.getParams().setParameter(ClientPNames.COOKIE_POLICY, org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY);

            HttpResponse resp = null;
            try {
                resp = client.execute(host,post,context);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(),e);
            }
            Map<String,String> rh = new HashMap<String,String>();
            for(Header h:resp.getAllHeaders()){
                rh.put(h.getName(),h.getValue());
            }
            try {
                HttpResponseBean r = new HttpResponseBean(resp.getStatusLine().getStatusCode(),rh,EntityUtils.toByteArray(resp.getEntity()));
                printIfError(u,r);
                return r;
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(),e);
            }
        }finally {
            post.releaseConnection();
        }
    }

    private String toString(byte [] b, String charset){
        if(b==null)
            return null;
        return toString(new ByteArrayInputStream(b),charset);
    }

    private String toString(InputStream is, String charset){
        if(is==null)
            return null;
        return IOUtils.toString(is,charset);
    }

    private Map<String,Object> toWire(Map<String,Object> m){
        return Objects.toWire(m);
    }

    private Map<String,Object> fromWire(Map<String,Object> m){
        return Objects.fromWire(m);
    }

    /**
     *
     * @param u
     * @param data
     * @return
     */
    public Map<String,Object> getJSON(String u, Map<String,Object> data){
        HttpResponseBean resp = get(u, data, new HashMap<String, String>());
        Map<String,Object> r = null;
        String s = toString(resp.getData(), "UTF-8");
        r = fromWire(evalJSON(s));
        return getResponseData(r);
    }

    /**
    * 
     * @param r
     * @return
     */
    protected Map<String,Object> getResponseData(Map<String,Object> r) {
        boolean success = (""+r.get("success")).equals("true");
        if(success){
            return (Map<String,Object>)r.get("data");
        }else{
            throw new RuntimeException("Error in response: "+r);
        }
    }

    private String toJSON(Map<String,Object> m){
        return JSONFormat.toJSON(m);
    }

    private Map<String,Object> evalJSON(String json){
        try {
            return JSONFormat.evalJSON(json);
        } catch (JSONFormatException e) {
            throw new RuntimeException(e.getMessage(),e);
        }
    }

    /**
     *
     * @param u
     * @param data
     * @return
     */
    public Map<String,Object> postJSON(String u, Map<String,Object> data){
        Map<String,String> headers = new HashMap<String,String>();
        headers.put("Content-Type","application/json");
        Map<String,Object> r = null;
        HttpResponseBean resp = post(u,
                new ByteArrayInputStream(toJSON(toWire(data)).getBytes(Charset.forName("UTF-8"))), headers);
        String s = toString(resp.getData(), "UTF-8");
        r = fromWire(evalJSON(s));

        return getResponseData(r);
    }

    /**
     *
     * @param u
     * @param data
     * @param name
     * @param contentType
     * @return
     */
    public Map<String,Object> uploadFileForJSON(String u, InputStream data, String name, String contentType){
        HttpResponseBean resp = uploadFile(u, data, name, contentType);
        Map<String,Object> r = null;
        String s = toString(resp.getData(), "UTF-8");
        r = fromWire(evalJSON(s));
        return getResponseData(r);
    }

    /**
     *
     */
    public static class HttpResponseBean{
        private int status;
        private Map<String,String> headers;
        private byte[] data;

        public HttpResponseBean(int status, Map<String, String> headers, byte[] data) {
            this.status = status;
            this.headers = headers;
            this.data = data;
        }

        public int getStatus() {
            return status;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public byte[] getData() {
            return data;
        }
    }

}
