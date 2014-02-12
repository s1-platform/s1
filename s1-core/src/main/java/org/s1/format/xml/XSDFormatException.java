package org.s1.format.xml;

/**
 * XSD format error
 */
public class XSDFormatException extends Exception {

    public XSDFormatException() {
        super();
    }

    public XSDFormatException(String message) {
        super(message);
    }

    public XSDFormatException(String message, Throwable cause) {
        super(message,cause);
    }

    public XSDFormatException(Throwable cause) {
        super(cause);
    }
}