package org.s1.script;

import org.mozilla.javascript.ast.FunctionNode;
import org.s1.objects.Objects;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 04.02.14
 * Time: 0:59
 */
public abstract class ScriptFunction implements Serializable {
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

    public Object call(Map<String,Object> m) throws JavaScriptException{
        List<Object> args = Objects.newArrayList();
        for(Object o:m.values()){
            args.add(o);
        }
        m.put("arguments",args);
        getContext().getVariables().putAll(m);
        return call();
    }

    public abstract Object call() throws JavaScriptException;

}
