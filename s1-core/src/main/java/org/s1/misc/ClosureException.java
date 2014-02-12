package org.s1.misc;

import org.s1.S1SystemError;

/**
 * Exception inside closure
 */
public class ClosureException extends Exception {

    /**
     *
     * @param e
     * @return
     */
    public static ClosureException wrap(Throwable e){
        return new ClosureException(e.getMessage(),e);
    }

    /**
     *
     * @param e
     * @return
     */
    public static Throwable getCause(Throwable e){
        if(e.getCause()==null)
            return e;
        return e.getCause();
    }

    /**
     *
     * @return
     */
    public S1SystemError toSystemError(){
        return S1SystemError.wrap(this.getCause());
    }

    public ClosureException() {
        super();
    }

    public ClosureException(String message) {
        super(message);
    }

    public ClosureException(String message, Throwable cause) {
        super(message,cause);
    }

    public ClosureException(Throwable cause) {
        super(cause);
    }

}
