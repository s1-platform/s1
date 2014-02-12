package org.s1.format.xml;

/**
 * XML format error
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