package org.s1.misc;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.01.14
 * Time: 18:46
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