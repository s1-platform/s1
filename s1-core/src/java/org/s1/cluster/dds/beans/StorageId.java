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

import org.s1.S1SystemError;
import org.s1.cluster.dds.DistributedDataSource;
import org.s1.objects.Objects;

import java.util.Map;

/**
 * Command bean
 */
public class StorageId extends Id {
    private Class<? extends DistributedDataSource> dataSource;

    /**
     *
     */
    public StorageId() {
    }

    /**
     *
     * @param dataSource
     * @param database
     * @param collection
     * @param entity
     */
    public StorageId(Class<? extends DistributedDataSource> dataSource, String database, String collection, String entity) {
        super(database,collection,entity);
        this.dataSource = dataSource;
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
    public String toString(){
        String s = "class: "+getDataSource()+", "+super.toString();
        return s;
    }

    public String getLockName(){
        if(Objects.isNullOrEmpty(getEntity())
                && Objects.isNullOrEmpty(getCollection())
                && Objects.isNullOrEmpty(getDatabase())){
            return ""+getDataSource();
        }else if(Objects.isNullOrEmpty(getEntity())
                && Objects.isNullOrEmpty(getCollection())){
            return ""+getDataSource()+"/"+getDatabase();
        }else if(Objects.isNullOrEmpty(getEntity())){
            return ""+getDataSource()+"/"+getDatabase()+"/"+getCollection();
        }else{
            return ""+getDataSource()+"/"+getDatabase()+"/"+getCollection()+"/"+getEntity();
        }
    }

    public void fromMap(Map<String,Object> m){
        super.fromMap(m);
        String cls = Objects.get(String.class, m, "dataSource");
        try{
            setDataSource((Class<? extends DistributedDataSource>)Class.forName(cls));
        }catch (Throwable e){
            throw S1SystemError.wrap(e);
        }
    }

    public Map<String,Object> toMap(){
        Map<String,Object> m = super.toMap();
        m.put("dataSource",getDataSource()==null?null:getDataSource().getName());
        return m;
    }

}
