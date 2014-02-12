package org.s1.script;

import org.s1.objects.Objects;

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
     * @throws ScriptException
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
