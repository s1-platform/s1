package org.s1.script;

/**
 * Will be thrown on
 * <code>continue;</code>
 * command
 */
public class LoopContinueException extends RuntimeException {

    public LoopContinueException() {
        super();
    }

}