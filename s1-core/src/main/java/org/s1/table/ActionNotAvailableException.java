package org.s1.table;

/**
 * Requested action not available
 */
public class ActionNotAvailableException extends Exception {

    public ActionNotAvailableException() {
        super();
    }

    public ActionNotAvailableException(String message) {
        super(message);
    }

    public ActionNotAvailableException(String message, Throwable cause) {
        super(message,cause);
    }

    public ActionNotAvailableException(Throwable cause) {
        super(cause);
    }
}