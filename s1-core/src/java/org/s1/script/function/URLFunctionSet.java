package org.s1.script.function;

import org.s1.S1SystemError;
import org.s1.objects.MapMethod;
import org.s1.objects.Objects;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

/**
 * @author Grigory Pykhov
 */
public class URLFunctionSet extends ScriptFunctionSet {

    @MapMethod
    public Map<String,Object> getParams(String url){
        Map<String,Object> m = Objects.newSOHashMap();

        String [] arr = url.split("\\?");
        String q = "";
        if(arr.length>1){
            q = arr[1];
        }

        String [] arr2 = q.split("&");
        String q2 = "";
        for(String s:arr2){
            String arr3 [] = s.split("=");
            String k = arr3[0];
            if(!Objects.isNullOrEmpty(k)) {
                String v = null;
                if (arr3.length > 1)
                    v = decode(arr3[1]);
                m.put(decode(k), v);
            }
        }
        return m;
    }

    @MapMethod
    public String setParams(String url, Map<String,Object> params){
        Map<String,Object> m = getParams(url);
        m.putAll(params);
        return replaceParams(url,m);
    }

    @MapMethod
    public String replaceParams(String url, Map<String,Object> params){
        String [] arr = url.split("\\?");
        String u = arr[0];
        Map<String,Object> m = params;
        int i=0;
        for(String k:m.keySet()){
            if(i==0)
                u+="?";
            else
                u+="&";
            u+=encode(k)+"="+encode(Objects.get(String.class,m,k));
            i++;
        }

        return u;
    }

    @MapMethod
    public String removeParams(String url, List<String> param){
        Map<String,Object> m = getParams(url);
        for(String k :param) {
            m.remove(k);
        }
        return replaceParams(url,m);
    }

    @MapMethod
    public String encode(String value){
        try {
            return URLEncoder.encode(value,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw S1SystemError.wrap(e);
        }
    }

    @MapMethod
    public String decode(String value){
        try {
            return URLDecoder.decode(value,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw S1SystemError.wrap(e);
        }
    }

}
