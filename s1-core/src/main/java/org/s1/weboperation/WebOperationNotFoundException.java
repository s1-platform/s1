package org.s1.weboperation;

/**
 * Web operation not found
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