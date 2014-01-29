package org.s1.objects.schema;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.01.14
 * Time: 18:46
 */
public class ObjectSchemaFormatException extends Exception {

    public ObjectSchemaFormatException() {
        super();
    }

    public ObjectSchemaFormatException(String message) {
        super(message);
    }

    public ObjectSchemaFormatException(String message, Throwable cause) {
        super(message,cause);
    }

    public ObjectSchemaFormatException(Throwable cause) {
        super(cause);
    }
}