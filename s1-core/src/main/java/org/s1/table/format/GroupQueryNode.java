package org.s1.table.format;

import org.s1.objects.Objects;

import java.util.List;
import java.util.Map;

/**
 * Group query node
 */
public class GroupQueryNode extends QueryNode{

    private GroupOperation operation = GroupOperation.AND;
    private List<QueryNode> children = Objects.newArrayList();

    /**
     *
     */
    public GroupQueryNode() {
    }

    /**
     *
     * @param operation
     * @param children
     */
    public GroupQueryNode(GroupOperation operation, List<QueryNode> children) {
        this.operation = operation;
        this.children = children;
    }

    /**
     *
     * @param operation
     * @param children
     */
    public GroupQueryNode(GroupOperation operation, QueryNode ... children) {
        this(operation,Objects.newArrayList(children));
    }

    /**
     *
     */
    public static enum GroupOperation{
        OR,AND
    }

    /**
     *
     * @return
     */
    public Map<String,Object> toMap(){
        Map<String,Object> m = super.toMap();
        List<Map<String,Object>> ch = Objects.newArrayList();
        m.put("children",ch);
        m.put("operation", operation==null?null:operation.toString().toLowerCase());
        for(QueryNode q : children){
            ch.add(q.toMap());
        }
        return m;
    }

    /**
     *
     */
    public void fromMap(Map<String,Object> m){
        super.fromMap(m);
        operation = GroupOperation.valueOf(Objects.get(String.class,m,"operation","and").toUpperCase());
        children.clear();
        List<Map<String,Object>> ch = Objects.get(m,"children");
        if(ch!=null){
            for(Map<String,Object> c:ch){
                children.add(createFromMap(c));
            }
        }
    }

    /**
     *
     * @return
     */
    public List<QueryNode> getChildren() {
        return children;
    }

    /**
     *
     * @return
     */
    public GroupOperation getOperation() {
        return operation;
    }

    /**
     *
     * @param operation
     */
    public void setOperation(GroupOperation operation) {
        this.operation = operation;
    }
}
