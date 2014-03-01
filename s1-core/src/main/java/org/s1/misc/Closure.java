/*
 * Copyright 2014 Grigory Pykhov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
