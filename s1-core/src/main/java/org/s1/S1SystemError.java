package org.s1;

/**
 * System error
 */
public class S1SystemError extends RuntimeException {

    /**
     * Wrap exception with system error
     *
     * @param e
     * @return
     */
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
