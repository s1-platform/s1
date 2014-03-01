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
