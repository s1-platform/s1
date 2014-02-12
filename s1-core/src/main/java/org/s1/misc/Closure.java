package org.s1.misc;

import org.s1.S1SystemError;

import java.io.Serializable;

/**
 * Abstract function class
 */
public abstract class Closure<I,O> {

    /**
     *
     * @param input
     * @return
     * @throws ClosureException
     */
    public abstract O call(I input) throws ClosureException;

    /**
     *
     * @param input
     * @return
     */
    public O callQuite(I input){
        try{
            return call(input);
        }catch (ClosureException e){
            throw e.toSystemError();
        }
    }

}
