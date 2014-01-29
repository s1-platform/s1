package org.s1.misc.protocols.classpath;

import org.s1.objects.Objects;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * s1
 * User: GPykhov
 * Date: 13.11.13
 * Time: 11:40
 */
public class Handler extends URLStreamHandler {
    /** The classloader to find resources from. */
    private final ClassLoader classLoader;

    public Handler() {
        this.classLoader = getClass().getClassLoader();
    }

    public Handler(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        String path = u.getPath();
        if(!Objects.isNullOrEmpty(u.getHost())){
            if(!path.startsWith("/"))
                path = "/"+path;
            path = u.getHost()+path;
        }
        if(path.startsWith("//")){
            path = path.substring(2);
        }else if(path.startsWith("/")){
            path = path.substring(1);
        }
        final URL resourceUrl = classLoader.getResource(path);
        try{
            return resourceUrl.openConnection();
        }catch (NullPointerException e){
            throw new IllegalArgumentException("Cannot open "+u.toString()+" connection",e);
        }
    }
}
