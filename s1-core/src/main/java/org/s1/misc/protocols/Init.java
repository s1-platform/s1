package org.s1.misc.protocols;

/**
 * Static initializer of custom protocol handlers
 */
public class Init {

    /**
     * Call this method once before using handlers
     */
    public static void init(){
        System.setProperty("java.protocol.handler.pkgs","org.s1.misc.protocols");
    }
}
