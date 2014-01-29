package org.s1.misc;

import org.s1.S1SystemError;

import java.io.Serializable;

/**
 * s1v2
 * User: GPykhov
 * Date: 09.01.14
 * Time: 21:06
 */
public abstract class Closure<I,O> {

    public abstract O call(I input) throws ClosureException;

    public O callQuite(I input){
        try{
            return call(input);
        }catch (ClosureException e){
            throw e.toSystemError();
        }
    }

}
