package org.s1.script;

import org.s1.objects.Objects;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.01.14
 * Time: 18:46
 */
public class JavaScriptException extends RuntimeException {
    private Object data;

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public JavaScriptException(Object data) {
        super();
        this.data = data;
    }

    public String getMessage(){
        return Objects.cast(data,String.class);
    }
}