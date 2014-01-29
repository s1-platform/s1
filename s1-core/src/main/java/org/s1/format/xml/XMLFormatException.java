package org.s1.format.xml;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.01.14
 * Time: 18:46
 */
public class XMLFormatException extends Exception {

    public XMLFormatException() {
        super();
    }

    public XMLFormatException(String message) {
        super(message);
    }

    public XMLFormatException(String message, Throwable cause) {
        super(message,cause);
    }

    public XMLFormatException(Throwable cause) {
        super(cause);
    }
}