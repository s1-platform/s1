package org.s1.table.format;

import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;
import org.s1.script.S1ScriptEngine;
import org.s1.script.ScriptException;
import org.s1.table.Table;

import java.util.Map;

/**
 * Universal query format
 */
public class Query {

    private QueryNode node = new GroupQueryNode();
    private Map<String,Object> custom;

    /**
     *
     * @param m
     */
    public void fromMap(Map<String,Object> m){
        Map<String,Object> n = Objects.get(m,"node");
        if(!Objects.isNullOrEmpty(n))
            node = QueryNode.createFromMap(n);
        custom = Objects.get(m,"custom");
    }

    /**
     *
     * @return
     */
    public Map<String,Object> toMap(){
        Map<String,Object> m = Objects.newHashMap(
                "node", node.toMap(),
                "custom", custom
        );
        return m;
    }

    /**
     *
     */
    public Query() {
    }

    /**
     *
     * @param node
     */
    public Query(QueryNode node) {
        this.node = node;
    }

    /**
     *
     * @param node
     * @param custom
     */
    public Query(QueryNode node, Map<String, Object> custom) {
        this.node = node;
        this.custom = custom;
    }

    /**
     *
     * @return
     */
    public QueryNode getNode() {
        return node;
    }

    /**
     *
     * @param node
     */
    public void setNode(QueryNode node) {
        this.node = node;
    }

    /**
     *
     * @return
     */
    public Map<String, Object> getCustom() {
        return custom;
    }

    /**
     *
     * @param custom
     */
    public void setCustom(Map<String, Object> custom) {
        this.custom = custom;
    }
}
