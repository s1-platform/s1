package org.s1.table.format;

import org.s1.objects.Objects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Query node
 */
public abstract class QueryNode {

    private boolean not;

    /**
     *
     * @param m
     * @return
     */
    public static QueryNode createFromMap(Map<String,Object> m){
        QueryNode qn = null;
        if(m.containsKey("children")){
            qn = new GroupQueryNode();
        }else{
            qn = new FieldQueryNode();
        }
        qn.fromMap(m);
        return qn;
    }

    /**
     *
     * @return
     */
    public Map<String,Object> toMap(){
        Map<String,Object> m = Objects.newHashMap(String.class, Object.class,
                "not", not);

        return m;
    }

    /**
     *
     */
    public void fromMap(Map<String,Object> m){
        not = Objects.get(Boolean.class,m,"not",false);
    }

    /**
     *
     * @return
     */
    public boolean isNot() {
        return not;
    }

    /**
     *
     * @param not
     */
    public void setNot(boolean not) {
        this.not = not;
    }
}
