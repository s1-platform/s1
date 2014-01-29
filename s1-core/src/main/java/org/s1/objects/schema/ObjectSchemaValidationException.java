package org.s1.objects.schema;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.01.14
 * Time: 18:46
 */
public class ObjectSchemaValidationException extends Exception {

    public ObjectSchemaValidationException() {
        super();
    }

    public ObjectSchemaValidationException(String message) {
        super(message);
    }

    public ObjectSchemaValidationException(String message, Throwable cause) {
        super(message,cause);
    }

    public ObjectSchemaValidationException(Throwable cause) {
        super(cause);
    }
}