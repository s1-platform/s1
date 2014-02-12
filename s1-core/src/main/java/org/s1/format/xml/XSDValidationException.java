package org.s1.format.xml;

/**
 * Error validating XML on XSD
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