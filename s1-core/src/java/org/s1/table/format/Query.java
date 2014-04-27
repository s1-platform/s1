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

package org.s1.table.format;

import org.s1.objects.Objects;

import java.util.Map;

/**
 * Universal query format
 */
public class Query {

    private QueryNode node = new GroupQueryNode();
    private Map<String,Object> custom;

    /**
     *
     * @param m
     */
    public void fromMap(Map<String,Object> m){
        Map<String,Object> n = Objects.get(m,"node");
        if(!Objects.isNullOrEmpty(n))
            node = QueryNode.createFromMap(n);
        custom = Objects.get(m,"custom");
    }

    /**
     *
     * @return
     */
    public Map<String,Object> toMap(){
        Map<String,Object> m = Objects.newHashMap(
                "node", node.toMap(),
                "custom", custom
        );
        return m;
    }

    /**
     *
     */
    public Query() {
    }

    /**
     *
     * @param node
     */
    public Query(QueryNode node) {
        this.node = node;
    }

    /**
     *
     * @param node
     * @param custom
     */
    public Query(QueryNode node, Map<String, Object> custom) {
        this.node = node;
        this.custom = custom;
    }

    /**
     *
     * @return
     */
    public QueryNode getNode() {
        return node;
    }

    /**
     *
     * @param node
     */
    public void setNode(QueryNode node) {
        this.node = node;
    }

    /**
     *
     * @return
     */
    public Map<String, Object> getCustom() {
        return custom;
    }

    /**
     *
     * @param custom
     */
    public void setCustom(Map<String, Object> custom) {
        this.custom = custom;
    }
}
