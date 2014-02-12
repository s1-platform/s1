package org.s1.misc;

/**
 * Base64 format error
 */
public class Base64FormatException extends Exception {

    public Base64FormatException() {
        super();
    }

    public Base64FormatException(String message) {
        super(message);
    }

    public Base64FormatException(String message, Throwable cause) {
        super(message,cause);
    }

    public Base64FormatException(Throwable cause) {
        super(cause);
    }
}