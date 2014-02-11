package org.s1.script;

/**
 * Will be thrown on
 * <code>break;</code>
 * command
 */
public class LoopBreakException extends RuntimeException {

    public LoopBreakException() {
        super();
    }

}