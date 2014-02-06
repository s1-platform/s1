package org.s1.script;

import org.mozilla.javascript.ast.FunctionNode;

import java.util.List;

/**
 * s1v2
 * User: GPykhov
 * Date: 04.02.14
 * Time: 0:59
 */
public abstract class ScriptFunction {
    private Context context;
    private List<String> params;

    public ScriptFunction(Context context, List<String> params) {
        this.context = context;
        this.params = params;
    }

    public List<String> getParams() {
        return params;
    }

    public Context getContext() {
        return context;
    }

    public abstract Object call() throws JavaScriptException;

}
