package org.s1.script.functions;

import org.s1.script.Context;

/**
 * s1v2
 * User: GPykhov
 * Date: 07.02.14
 * Time: 15:43
 */
public abstract class ScriptFunctions {

    private Context context = new Context();

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
