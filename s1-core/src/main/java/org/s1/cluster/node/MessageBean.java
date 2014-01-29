package org.s1.cluster.node;

import java.io.Serializable;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 20.01.14
 * Time: 10:04
 */
public class MessageBean extends CommandBean{
    private long id;
    private String nodeId;
    private String group;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String toString(){
        return toString(true);
    }

    public String toString(boolean withData){
        String s = "id:"+getId()+", class:"+getClass().getName()+", command:"+getCommand()+", group:"+getGroup();
        if(withData)
            s+=", params:"+getParams();
        return s;
    }
}
