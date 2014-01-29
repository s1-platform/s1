package org.s1.format.xml;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.01.14
 * Time: 18:46
 */
public class XSDValidationException extends Exception {

    public XSDValidationException() {
        super();
    }

    public XSDValidationException(String message) {
        super(message);
    }

    public XSDValidationException(String message, Throwable cause) {
        super(message,cause);
    }

    public XSDValidationException(Throwable cause) {
        super(cause);
    }
}