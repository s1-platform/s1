package org.s1.script;

import org.s1.objects.Objects;

/**
 * Script exception.
 * Will be thrown on
 * <code>throw ...;</code>
 * command or on some runtime error
 */
public class ScriptException extends RuntimeException {
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

    public String getMessage(){
        return Objects.cast(data,String.class);
    }
}