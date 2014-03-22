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

import org.s1.objects.Objects;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Command bean
 */
public class EntityIdBean implements Serializable{
    private Class<? extends DistributedDataSource> dataSource;
    private String database;
    private String collection;
    private String entity;

    /**
     *
     */
    public EntityIdBean() {
    }

    /**
     *
     * @param dataSource
     * @param database
     * @param collection
     * @param entity
     */
    public EntityIdBean(Class<? extends DistributedDataSource> dataSource, String database, String collection, String entity) {
        this.dataSource = dataSource;
        this.database = database;
        this.collection = collection;
        this.entity = entity;
    }

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
    public String getDatabase() {
        return database;
    }

    /**
     *
     * @param database
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     *
     * @return
     */
    public String getCollection() {
        return collection;
    }

    /**
     *
     * @param collection
     */
    public void setCollection(String collection) {
        this.collection = collection;
    }

    /**
     *
     * @return
     */
    public String getEntity() {
        return entity;
    }

    /**
     *
     * @param entity
     */
    public void setEntity(String entity) {
        this.entity = entity;
    }

    /**
     *
     * @return
     */
    public String toString(){
        String s = "class: "+getDataSource()+", database: "+getDatabase()
                +", collection: "+getCollection()+", entity: "+getEntity();
        return s;
    }

    public String getLockName(){
        String s = ""+getDataSource()+"/"+getDatabase()
                +"/"+getCollection()+"/"+getEntity();
        return s;
    }

}
