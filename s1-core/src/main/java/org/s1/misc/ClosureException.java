package org.s1.misc;

import org.s1.S1SystemError;

/**
 * s1v2
 * User: GPykhov
 * Date: 23.01.14
 * Time: 14:57
 */
public class ClosureException extends Exception {

    public static ClosureException wrap(Throwable e){
        return new ClosureException(e.getMessage(),e);
    }

    public static Throwable getCause(Throwable e){
        if(e.getCause()==null)
            return e;
        return e.getCause();
    }

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
