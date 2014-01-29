package org.s1.cluster;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.01.14
 * Time: 18:46
 */
public class AccessDeniedException extends Exception {

    public AccessDeniedException() {
        super();
    }

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException(String message, Throwable cause) {
        super(message,cause);
    }

    public AccessDeniedException(Throwable cause) {
        super(cause);
    }
}