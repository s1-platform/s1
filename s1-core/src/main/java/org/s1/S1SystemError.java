package org.s1;

/**
 * s1v2
 * User: GPykhov
 * Date: 23.01.14
 * Time: 14:57
 */
public class S1SystemError extends RuntimeException {

    public static S1SystemError wrap(Throwable e){
        return new S1SystemError(e.getMessage(),e);
    }

    public S1SystemError() {
        super();
    }

    public S1SystemError(String message) {
        super(message);
    }

    public S1SystemError(String message, Throwable cause) {
        super(message,cause);
    }

    public S1SystemError(Throwable cause) {
        super(cause);
    }

}
