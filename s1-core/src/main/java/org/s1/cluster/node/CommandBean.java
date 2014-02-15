package org.s1.cluster.node;

import org.s1.S1SystemError;
import org.s1.cluster.datasource.DistributedDataSource;
import org.s1.objects.Objects;

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

    /**
     *
     * @param m
     */
    public void fromMap(Map<String,Object> m){
        System.out.println(m);
        String cl_name = Objects.get(m, "class");
        Class<? extends DistributedDataSource> cls = null;
        if(cl_name!=null){
            try{
                cls = (Class<? extends DistributedDataSource>)Class.forName(cl_name);
            }catch (Exception e){
                throw S1SystemError.wrap(e);
            }
        }
        setDataSource(cls);
        setCommand(Objects.get(String.class,m,"command"));
        setParams(Objects.get(Map.class,m,"params"));
    }

    /**
     *
     * @return
     */
    public Map<String,Object> toMap(){
        Map<String,Object> m = Objects.newHashMap();
        m.put("class",getDataSource()!=null?getDataSource().getName():null);
        m.put("command",getCommand());
        m.put("params",getParams());
        return m;
    }
}
