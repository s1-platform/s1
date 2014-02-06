package org.s1.script;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.01.14
 * Time: 18:46
 */
public class LoopContinueException extends RuntimeException {

    public LoopContinueException() {
        super();
    }

    public LoopContinueException(String message) {
        super(message);
    }

    public LoopContinueException(String message, Throwable cause) {
        super(message,cause);
    }

    public LoopContinueException(Throwable cause) {
        super(cause);
    }
}