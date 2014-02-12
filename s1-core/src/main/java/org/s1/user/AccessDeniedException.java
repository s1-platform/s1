package org.s1.user;

/**
 * Access denied exception
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