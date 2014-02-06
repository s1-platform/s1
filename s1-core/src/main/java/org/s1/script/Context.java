package org.s1.script;

import org.s1.objects.Objects;

import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 01.02.14
 * Time: 10:40
 */
public class Context {
    private Map<String,Object> variables = Objects.newHashMap();
    private Context parent;
    private List<Context> children = Objects.newArrayList();

    public Context(){

    }

    public Context createChild(){
        Context c = new Context();
        c.parent = this;
        this.children.add(c);
        return c;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public Context getParent() {
        return parent;
    }

    public List<Context> getChildren() {
        return children;
    }

    public Object get(String name){
        Map<String,Object> m = getMap(name);
        if(m!=null)
            return m.get(name);
        return null;
    }

    public void set(String name, Object o){
        Map<String,Object> m = getMap(name);
        if(m!=null)
            m.put(name, o);
    }

    private Map<String,Object> getMap(String name){
        if(variables.containsKey(name))
            return variables;
        else if(parent!=null)
            return parent.getMap(name);
        return null;
    }
}
