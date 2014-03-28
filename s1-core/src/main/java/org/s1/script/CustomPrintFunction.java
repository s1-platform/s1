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

package org.s1.script;

import org.s1.objects.Objects;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Custom print
 */
public abstract class CustomPrintFunction extends ScriptFunction {

    public CustomPrintFunction(Context context) {
        super(context,Objects.newArrayList("text"));
    }


    public Object call() throws ScriptException{
        String text = getContext().get("text");
        return print(text);
    }

    public abstract String print(String text) throws ScriptException;

}
