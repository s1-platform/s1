package org.s1.script;

import org.s1.objects.Objects;

/**
 * Will be thrown on
 * <code>return ...;</code>
 * command
 */
public class FunctionReturnException extends RuntimeException {

    private Object data;

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public FunctionReturnException(Object data) {
        super();
        this.data = data;
    }

    public String getMessage(){
        return Objects.cast(data,String.class);
    }
}