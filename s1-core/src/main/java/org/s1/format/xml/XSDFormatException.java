package org.s1.format.xml;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.01.14
 * Time: 18:46
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