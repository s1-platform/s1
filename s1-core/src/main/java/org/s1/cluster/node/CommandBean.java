package org.s1.cluster.node;

import org.s1.cluster.datasource.DistributedDataSource;

import java.io.Serializable;
import java.util.Map;

/**
 * Command bean
 */
public class CommandBean implements Serializable{
    private Class<? extends DistributedDataSource> dataSource;
    private String command;
    private Map<String,Object> params;

    /**
     *
     * @return
     */
    public Class<? extends DistributedDataSource> getDataSource() {
        return dataSource;
    }

    /**
     *
     * @param dataSource
     */
    public void setDataSource(Class<? extends DistributedDataSource> dataSource) {
        this.dataSource = dataSource;
    }

    /**
     *
     * @return
     */
    public String getCommand() {
        return command;
    }

    /**
     *
     * @param command
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     *
     * @return
     */
    public Map<String, Object> getParams() {
        return params;
    }

    /**
     *
     * @param params
     */
    public void setParams(Map<String, Object> params) {
        this.params = params;
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
        String s = "class: "+getDataSource()+", command: "+getCommand();
        if(withData)
            s+=", params: "+getParams();
        return s;
    }
}
