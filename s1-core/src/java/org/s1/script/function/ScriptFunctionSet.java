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

import org.s1.objects.MapMethodWrapper;
import org.s1.objects.Objects;
import org.s1.script.Context;
import org.s1.script.errors.ScriptException;
import org.s1.script.errors.ScriptLimitException;
import org.s1.script.errors.SyntaxException;

import java.util.List;
import java.util.Map;

/**
 * Base class for script functions
 */
public abstract class ScriptFunctionSet {

    private Context context;
    private Map<String,Object> config;

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    /**
     * Get function context
     *
     * @return
     */
    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Object callFunction(String method, List<Object> args) throws Exception{
        return MapMethodWrapper.findAndInvoke(this,method,args);
    }

    public ScriptFunction getFunction(final String name){
        return new ScriptFunction(getContext(), Objects.newArrayList(String.class)) {

            @Override
            public Object call(Context ctx) throws ScriptException {
                List<Object> args = ctx.get("arguments");
                try {
                    return callFunction(name,args);
                } catch (ScriptException e) {
                    throw e;
                } catch (SyntaxException e) {
                    throw e;
                } catch (ScriptLimitException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ScriptException(e.getMessage(),e);
                }
            }
        };
    }
}
