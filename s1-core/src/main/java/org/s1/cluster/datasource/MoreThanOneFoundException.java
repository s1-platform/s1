package org.s1.cluster.datasource;

/**
 * More than one object found
 */
public class MoreThanOneFoundException extends Exception {

    public MoreThanOneFoundException() {
        super();
    }

    public MoreThanOneFoundException(String message) {
        super(message);
    }

    public MoreThanOneFoundException(String message, Throwable cause) {
        super(message,cause);
    }

    public MoreThanOneFoundException(Throwable cause) {
        super(cause);
    }
}