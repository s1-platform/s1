package org.s1.weboperation;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.01.14
 * Time: 18:46
 */
public class MethodNotFoundException extends Exception {

    public MethodNotFoundException() {
        super();
    }

    public MethodNotFoundException(String message) {
        super(message);
    }

    public MethodNotFoundException(String message, Throwable cause) {
        super(message,cause);
    }

    public MethodNotFoundException(Throwable cause) {
        super(cause);
    }
}