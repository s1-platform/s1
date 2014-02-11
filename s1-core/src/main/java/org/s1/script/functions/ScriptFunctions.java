package org.s1.script.functions;

import org.s1.script.Context;

/**
 * Base class for script functions
 */
public abstract class ScriptFunctions {

    private Context context;

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
}
