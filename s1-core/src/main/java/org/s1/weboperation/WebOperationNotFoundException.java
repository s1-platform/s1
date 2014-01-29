package org.s1.weboperation;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.01.14
 * Time: 18:46
 */
public class WebOperationNotFoundException extends Exception {

    public WebOperationNotFoundException() {
        super();
    }

    public WebOperationNotFoundException(String message) {
        super(message);
    }

    public WebOperationNotFoundException(String message, Throwable cause) {
        super(message,cause);
    }

    public WebOperationNotFoundException(Throwable cause) {
        super(cause);
    }
}