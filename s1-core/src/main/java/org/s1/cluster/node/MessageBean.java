package org.s1.cluster.node;

import org.s1.S1SystemError;
import org.s1.cluster.datasource.DistributedDataSource;
import org.s1.objects.Objects;

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
        String s = "id:"+getId()+",nodeId: "+getNodeId()+
                ", class:"+(getDataSource()!=null?getDataSource().getName():null)+
                ", command:"+getCommand()+", group:"+getGroup();
        if(withData)
            s+=", params:"+getParams();
        return s;
    }

    /**
     *
     * @param m
     */
    public void fromMap(Map<String,Object> m){
        super.fromMap(m);
        id = Objects.get(m,"id");
        nodeId = Objects.get(m,"nodeId");
        group = Objects.get(m,"group");
    }

    /**
     *
     * @return
     */
    public Map<String,Object> toMap(){
        Map<String,Object> m = super.toMap();
        m.put("id",id);
        m.put("nodeId",nodeId);
        m.put("group",group);
        return m;
    }
}
