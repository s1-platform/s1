/*
 * Copyright 2014 Grigory Pykhov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.s1.cluster.dds.beans;

import org.s1.cluster.dds.DistributedDataSource;
import org.s1.objects.Objects;

import java.util.List;
import java.util.Map;

/**
 * Command bean
 */
public class CommandBean extends StorageId {
    private String command;
    private Map<String,Object> params;

    /**
     *
     */
    public CommandBean() {
    }

    /**
     *
     * @param dataSource
     * @param database
     * @param collection
     * @param entity
     * @param command
     * @param params
     */
    public CommandBean(Class<? extends DistributedDataSource> dataSource, String database, String collection, String entity, String command, Map<String, Object> params) {
        super(dataSource, database, collection, entity);
        this.command = command;
        this.params = params;
    }

    public boolean isSameEntity(StorageId entityId){
        if(getDataSource()==null){
            //trans
            List<CommandBean> l = Objects.get(this.getParams(), "list");
            for(CommandBean c:l){
                if(c.getDataSource().getName().equals(entityId.getDataSource().getName())
                        && c.getDatabase().equals(entityId.getDatabase())
                        && c.getCollection().equals(entityId.getCollection())
                        && c.getEntity().equals(entityId.getEntity())){
                    return true;
                }
            }
            return false;
        }else{
            return this.getDataSource().getName().equals(entityId.getDataSource().getName())
                    && this.getDatabase().equals(entityId.getDatabase())
                    && this.getCollection().equals(entityId.getCollection())
                    && this.getEntity().equals(entityId.getEntity());
        }
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
        String s = super.toString()+", command: "+getCommand();
        if(withData)
            s+=", params: "+getParams();
        return s;
    }

}
