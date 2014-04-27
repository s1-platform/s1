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

package org.s1.misc.protocols.classpath;

import org.s1.objects.Objects;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Classpath handler
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
