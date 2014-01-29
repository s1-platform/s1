package org.s1.format.json;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.01.14
 * Time: 18:46
 */
public class JSONFormatException extends Exception {

    public JSONFormatException() {
        super();
    }

    public JSONFormatException(String message) {
        super(message);
    }

    public JSONFormatException(String message, Throwable cause) {
        super(message,cause);
    }

    public JSONFormatException(Throwable cause) {
        super(cause);
    }
}