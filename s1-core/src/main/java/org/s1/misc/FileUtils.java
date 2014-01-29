package org.s1.misc;

import org.s1.S1SystemError;

import java.io.*;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 13:35
 */
public class FileUtils {

    /**
     *
     * @param dir
     */
    public static void deleteDir(File dir){
        if(dir.isDirectory() && dir.exists()){
            for(File f:dir.listFiles()){
                if(f.isDirectory()){
                    deleteDir(f);
                    f.delete();
                }
                f.delete();
            }
        }
        dir.delete();
    }

    /**
     *
     * @param src
     * @param dst
     */
    public static void copyFile(File src, File dst) throws IOException{
        OutputStream os = null;
        InputStream is = null;
        try{
            os = new FileOutputStream(dst);
            is = new FileInputStream(src);
            IOUtils.copy(is,os);
        }finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
    }

    /**
     *
     * @param f
     * @param s
     * @param encoding
     */
    public static void writeStringToFile(File f, String s, String encoding) throws IOException{
        OutputStream os = null;
        try{
            os = new FileOutputStream(f);
            os.write(s.getBytes(encoding));
        }finally {
            IOUtils.closeQuietly(os);
        }
    }

    /**
     *
     * @param f
     * @param encoding
     * @return
     */
    public static String readFileToString(File f, String encoding) throws IOException{
        String s = null;
        if(!f.exists() || f.isDirectory())
            return s;
        InputStream is = null;
        try{
            is = new FileInputStream(f);
            s = IOUtils.toString(is,encoding);
        }finally {
            IOUtils.closeQuietly(is);
        }
        return s;
    }

}
