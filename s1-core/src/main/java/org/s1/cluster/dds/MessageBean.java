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

package org.s1.cluster.dds;


import java.util.Map;

/**
 * Message bean
 */
public class MessageBean extends CommandBean{
    private long id;
    private String nodeId;

    /**
     *
     */
    public MessageBean() {
        super();
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
    public MessageBean(Class<? extends DistributedDataSource> dataSource, String database, String collection, String entity, String command, Map<String, Object> params) {
        super(dataSource, database, collection, entity, command, params);
    }

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
    public String toString(){
        return toString(true);
    }

    /**
     *
     * @param withData
     * @return
     */
    public String toString(boolean withData){
        String s = "id:"+getId()+",nodeId: "+getNodeId()+","+
                super.toString(withData);
        return s;
    }

}
