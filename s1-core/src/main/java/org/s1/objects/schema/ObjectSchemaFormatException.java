package org.s1.objects.schema;

/**
 * Object schema format exception
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