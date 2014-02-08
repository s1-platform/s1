package org.s1.script;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.01.14
 * Time: 18:46
 */
public class MemoryLimitException extends RuntimeException {

    public MemoryLimitException() {
        super();
    }

    public MemoryLimitException(String message) {
        super(message);
    }

    public MemoryLimitException(String message, Throwable cause) {
        super(message,cause);
    }

    public MemoryLimitException(Throwable cause) {
        super(cause);
    }
}