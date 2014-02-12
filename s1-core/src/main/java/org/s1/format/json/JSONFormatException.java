package org.s1.format.json;

/**
 * JSON format error
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