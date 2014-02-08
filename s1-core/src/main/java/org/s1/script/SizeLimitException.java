package org.s1.script;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.01.14
 * Time: 18:46
 */
public class SizeLimitException extends RuntimeException {

    public SizeLimitException() {
        super();
    }

    public SizeLimitException(String message) {
        super(message);
    }

    public SizeLimitException(String message, Throwable cause) {
        super(message,cause);
    }

    public SizeLimitException(Throwable cause) {
        super(cause);
    }
}