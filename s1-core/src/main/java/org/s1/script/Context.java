package org.s1.script;

import org.s1.objects.Objects;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Javascript context
 */
public class Context implements Serializable {
    private Map<String,Object> variables = Objects.newHashMap();
    private Context parent;
    private List<Context> children = Objects.newArrayList();
    private MemoryHeap memoryHeap;

    private Context(){

    }

    /**
     * Create new context
     *
     * @param memoryLimit
     */
    public Context(long memoryLimit){
        memoryHeap = new MemoryHeap(memoryLimit);
    }

    /**
     * Create child context
     *
     * @return
     */
    public Context createChild(){
        Context c = new Context();
        c.memoryHeap = memoryHeap;
        c.parent = this;
        this.children.add(c);
        return c;
    }

    public MemoryHeap getMemoryHeap() {
        return memoryHeap;
    }

    /**
     * Context variables
     *
     * @return
     */
    public Map<String, Object> getVariables() {
        return variables;
    }

    public Context getParent() {
        return parent;
    }

    public List<Context> getChildren() {
        return children;
    }

    /**
     * Find and get parameter from this context and upwards
     *
     * @param name
     * @param <T>
     * @return
     */
    public <T> T get(String name){
        Map<String,Object> m = getMap(name);
        if(m!=null)
            return (T)m.get(name);
        return null;
    }

    /**
     * Find and get parameter from this context and upwards
     *
     * @param c cast to this class
     * @param name
     * @param <T>
     * @return
     */
    public <T> T get(Class<T> c, String name){
        return Objects.cast(get(name),c);
    }

    /**
     * Find and set parameter from this context and upwards
     *
     * @param name
     * @param o
     */
    public void set(String name, Object o){
        Map<String,Object> m = getMap(name);
        if(m!=null)
            m.put(name, o);
    }

    /**
     * Find and remove parameter from this context and upwards
     *
     * @param name
     */
    public void remove(String name){
        Map<String,Object> m = getMap(name);
        if(m!=null)
            m.remove(name);
    }

    /**
     * Find map containing parameter from this context and upwards
     *
     * @param name
     * @return
     */
    public Map<String,Object> getMap(String name){
        if(variables.containsKey(name))
            return variables;
        else if(parent!=null)
            return parent.getMap(name);
        return null;
    }
}
