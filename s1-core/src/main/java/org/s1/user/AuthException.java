package org.s1.user;

/**
 * Authentication exception
 */
public class AuthException extends Exception {

    public AuthException() {
        super();
    }

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message,cause);
    }

    public AuthException(Throwable cause) {
        super(cause);
    }
}