package org.s1.cluster.datasource;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.01.14
 * Time: 18:46
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