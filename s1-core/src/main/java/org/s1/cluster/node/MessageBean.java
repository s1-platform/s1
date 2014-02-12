package org.s1.cluster.node;

import java.io.Serializable;
import java.util.Map;

/**
 * Message bean
 */
public class MessageBean extends CommandBean{
    private long id;
    private String nodeId;
    private String group;

    /**
     *
     * @return
     */
    public long getId() {
        return id;
    }

    /**
     *
     * @param id
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     *
     * @return
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     *
     * @param nodeId
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     *
     * @return
     */
    public String getGroup() {
        return group;
    }

    /**
     *
     * @param group
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     *
     * @return
     */
    public String toString(){
        return toString(true);
    }

    /**
     *
     * @param withData
     * @return
     */
    public String toString(boolean withData){
        String s = "id:"+getId()+", class:"+getClass().getName()+", command:"+getCommand()+", group:"+getGroup();
        if(withData)
            s+=", params:"+getParams();
        return s;
    }
}
