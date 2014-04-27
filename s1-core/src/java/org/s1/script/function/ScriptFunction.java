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

package org.s1.script.function;

import org.s1.objects.Objects;
import org.s1.script.Context;
import org.s1.script.errors.ScriptException;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Javascript functions class
 */
public abstract class ScriptFunction implements Serializable {
    private Context context;
    private List<String> params;

    public ScriptFunction(Context context, List<String> params) {
        this.context = context;
        this.params = params;
    }

    /**
     * Parameter names
     *
     * @return
     */
    public List<String> getParams() {
        return params;
    }

    /**
     * Call context, useful for closures
     *
     * @return
     */
    public Context getContext() {
        return context;
    }

    /**
     * Call with parameters
     *
     * @param m put this in context
     * @return
     * @throws org.s1.script.errors.ScriptException
     */
    public Object call(Map<String,Object> m) throws ScriptException {
        List<Object> args = Objects.newArrayList();
        for(Object o:m.values()){
            args.add(o);
        }
        m.put("arguments",args);
        getContext().getVariables().putAll(m);
        return call();
    }

    /**
     * Call function, parameters are already in context
     *
     * @return
     * @throws ScriptException
     */
    public abstract Object call() throws ScriptException;

}
