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

package org.s1.misc;

import java.io.*;

/**
 * File utils
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
