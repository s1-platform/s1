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

package org.s1.script.errors;

import org.s1.objects.Objects;

/**
 * Script exception.
 * Will be thrown on
 * <code>throw ...;</code>
 * command or on some runtime error
 */
public class ScriptException extends RuntimeException {

    private int line = -1;
    private int column;
    public String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getRawMessage(){
        return super.getMessage();
    }

    @Override
    public String getMessage() {
        String m = super.getMessage();
        if(line>=0)
            m+=" line "+(line+1)+", column "+column;
        if(!Objects.isNullOrEmpty(code))
            m+="\n"+code;
        return m;
    }

    private Object data;

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public ScriptException(Object data) {
        super(""+data);
        this.data = data;
    }

    public ScriptException(Object data, Throwable cause) {
        super(""+data,cause);
        this.data = data;
    }

    public ScriptException(String code, int line, int column, Object data) {
        super(""+data);
        this.code = code;
        this.data = data;
        this.line = line;
        this.column = column;
    }

    public ScriptException(String code, int line, int column, Object data, Throwable cause) {
        super(""+data,cause);
        this.code = code;
        this.data = data;
        this.line = line;
        this.column = column;
    }
}