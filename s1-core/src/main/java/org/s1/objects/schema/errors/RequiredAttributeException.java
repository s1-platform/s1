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

package org.s1.objects.schema.errors;

/**
 * Required attribute missing
 */
public class RequiredAttributeException extends ValidationException {

    public RequiredAttributeException() {
        super();
    }

    public RequiredAttributeException(String message) {
        super(message);
    }

    public RequiredAttributeException(String message, Throwable cause) {
        super(message,cause);
    }

    public RequiredAttributeException(Throwable cause) {
        super(cause);
    }
}