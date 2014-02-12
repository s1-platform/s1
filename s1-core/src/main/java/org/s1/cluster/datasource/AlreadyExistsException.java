package org.s1.cluster.datasource;

/**
 * Object already exists exception
 */
public class AlreadyExistsException extends Exception {

    public AlreadyExistsException() {
        super();
    }

    public AlreadyExistsException(String message) {
        super(message);
    }

    public AlreadyExistsException(String message, Throwable cause) {
        super(message,cause);
    }

    public AlreadyExistsException(Throwable cause) {
        super(cause);
    }
}