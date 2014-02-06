package org.s1.script;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.01.14
 * Time: 18:46
 */
public class LoopBreakException extends RuntimeException {

    public LoopBreakException() {
        super();
    }

    public LoopBreakException(String message) {
        super(message);
    }

    public LoopBreakException(String message, Throwable cause) {
        super(message,cause);
    }

    public LoopBreakException(Throwable cause) {
        super(cause);
    }
}