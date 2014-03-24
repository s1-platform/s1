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

import java.io.Serializable;

/**
 * Command bean
 */
public class CollectionId implements Serializable{
    private String database;
    private String collection;

    /**
     *
     */
    public CollectionId() {
    }

    /**
     *
     * @param database
     * @param collection
     */
    public CollectionId(String database, String collection) {
        this.database = database;
        this.collection = collection;
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
    public String toString(){
        String s = "database: "+getDatabase()
                +", collection: "+getCollection();
        return s;
    }

}
