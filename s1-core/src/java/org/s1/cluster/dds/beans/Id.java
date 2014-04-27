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

/**
 * Command bean
 */
public class Id extends CollectionId {
    private String entity;

    /**
     *
     */
    public Id() {
    }

    /**
     *
     * @param database
     * @param collection
     * @param entity
     */
    public Id(String database, String collection, String entity) {
        super(database,collection);
        this.entity = entity;
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
        String s = super.toString()+", entity: "+getEntity();
        return s;
    }

}
