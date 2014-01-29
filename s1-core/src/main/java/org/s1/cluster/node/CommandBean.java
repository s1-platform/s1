package org.s1.cluster.node;

import org.s1.cluster.datasource.DistributedDataSource;

import java.io.Serializable;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 20.01.14
 * Time: 10:04
 */
public class CommandBean implements Serializable{
    private Class<? extends DistributedDataSource> dataSource;
    private String command;
    private Map<String,Object> params;

    public Class<? extends DistributedDataSource> getDataSource() {
        return dataSource;
    }

    public void setDataSource(Class<? extends DistributedDataSource> dataSource) {
        this.dataSource = dataSource;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public String toString(){
        return toString(true);
    }

    public String toString(boolean withData){
        String s = "class: "+getDataSource()+", command: "+getCommand();
        if(withData)
            s+=", params: "+getParams();
        return s;
    }
}
