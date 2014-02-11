package org.s1.script;

import org.s1.objects.Objects;

/**
 * Script syntax exception.
 * Will be thrown if building ast goes wrong
 */
public class SyntaxException extends RuntimeException {

    public SyntaxException() {
        super();
    }

    public SyntaxException(String message) {
        super(message);
    }

    public SyntaxException(String message, Throwable cause) {
        super(message,cause);
    }

    public SyntaxException(Throwable cause) {
        super(cause);
    }

}