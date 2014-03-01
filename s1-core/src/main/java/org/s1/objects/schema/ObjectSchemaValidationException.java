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