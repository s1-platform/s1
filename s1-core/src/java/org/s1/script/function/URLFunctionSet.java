package org.s1.script.function;

import org.s1.S1SystemError;
import org.s1.objects.MapMethod;
import org.s1.objects.Objects;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * @author Grigory Pykhov
 */
public class URLFunctionSet extends ScriptFunctionSet {

    @MapMethod
    public String addParam(String url, String param, String value){
        String [] arr = url.split("\\?");
        String q = "";
        if(arr.length>1){
            q = arr[1];
        }
        if(q.length()!=0)
            q+="&";
        q+=param+"="+encode(value);
        return arr[0]+"?"+q;
    }

    @MapMethod
    public String removeParam(String url, String param){
        String [] arr = url.split("\\?");
        String q = "";
        if(arr.length>1){
            q = arr[1];
        }
        String [] arr2 = q.split("&");
        String q2 = "";
        for(String s:arr2){
            String arr3 [] = s.split("=");
            if(!param.equals(arr3[0]) && arr3.length>1){
                if(q2.length()!=0)
                    q2+="&";
                q2+=arr3[0]+"="+encode(arr3[1]);
            }
        }

        return arr[0]+(Objects.isNullOrEmpty(q2)?"":("?"+q2));
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
