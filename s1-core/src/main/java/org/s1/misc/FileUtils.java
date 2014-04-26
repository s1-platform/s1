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

import org.s1.objects.Objects;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * File utils
 */
public class FileUtils {

    public static List<String> getClasspathResources(String path){
        String pkgname = path.replace('/','.');
        if(pkgname.startsWith("."))
            pkgname = pkgname.substring(1);
        if(pkgname.endsWith("."))
            pkgname = pkgname.substring(0,pkgname.length()-1);

        List<String> res = Objects.newArrayList();
        // Get a File object for the package
        File directory = null;
        String fullPath;
        String relPath = pkgname.replace('.', '/');
        //System.out.println("ClassDiscovery: Package: " + pkgname + " becomes Path:" + relPath);
        URL resource = ClassLoader.getSystemClassLoader().getResource(relPath);
        //System.out.println("ClassDiscovery: Resource = " + resource);
        if (resource == null) {
            throw new RuntimeException("No resource for " + relPath);
        }
        fullPath = resource.getFile();
        //System.out.println("ClassDiscovery: FullPath = " + resource);

        try {
            directory = new File(resource.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(pkgname + " (" + resource + ") does not appear to be a valid URL / URI.  Strange, since we got it from the system...", e);
        } catch (IllegalArgumentException e) {
            directory = null;
        }
        //System.out.println("ClassDiscovery: Directory = " + directory);

        if (directory != null && directory.exists()) {
            // Get the list of the files contained in the package
            String[] files = directory.list();
            for (int i = 0; i < files.length; i++) {
                res.add(files[i]);
            }
        }
        else {
            try {
                String jarPath = fullPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
                JarFile jarFile = new JarFile(jarPath);
                Enumeration<JarEntry> entries = jarFile.entries();
                while(entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if(entryName.startsWith(relPath) && entryName.length() > (relPath.length() + "/".length())) {
                        res.add(entryName.substring(relPath.length()+1));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(pkgname + " (" + directory + ") does not appear to be a valid package", e);
            }
        }
        return res;
    }

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
