package org.s1.objects;

/**
 * @author Grigory Pykhov (grigoryp@hflabs.ru)
 */
public class EscapeUtils {

    public static String escapeXML(String s){
        String arr [][]= new String[][]{
                {"&","&amp;"},
                {"\"","&quot;"},
                {"'","&apos;"},
                {"<","&lt;"},
                {">","&gt;"}
        };
        for(String[] r:arr){
            s = s.replace(r[0],r[1]);
        }
        return s;
    }

    public static String escapeHTML(String s){
        String arr [][]= new String[][]{
                {"&","&amp;"},
                {"\"","&quot;"},
                {"'","&apos;"},
                {"<","&lt;"},
                {">","&gt;"}
        };
        for(String[] r:arr){
            s = s.replace(r[0],r[1]);
        }
        return s;
    }

    public static String escapeJS(String s){
        String arr [][]= new String[][]{
                {"\\","\\\\"},
                {"\"","\\\""},
                {"'","\\'"}
        };
        for(String[] r:arr){
            s = s.replace(r[0],r[1]);
        }
        return s;
    }

}
