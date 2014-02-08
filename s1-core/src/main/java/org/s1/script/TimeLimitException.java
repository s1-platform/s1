package org.s1.script;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.01.14
 * Time: 18:46
 */
public class TimeLimitException extends RuntimeException {

    public TimeLimitException() {
        super();
    }

    public TimeLimitException(String message) {
        super(message);
    }

    public TimeLimitException(String message, Throwable cause) {
        super(message,cause);
    }

    public TimeLimitException(Throwable cause) {
        super(cause);
    }
}