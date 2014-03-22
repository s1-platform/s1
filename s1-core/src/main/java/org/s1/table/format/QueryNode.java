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
 * Query node
 */
public abstract class QueryNode {

    private boolean not;

    /**
     *
     * @param m
     * @return
     */
    public static QueryNode createFromMap(Map<String,Object> m){
        QueryNode qn = null;
        if(m.containsKey("children")){
            qn = new GroupQueryNode();
        }else{
            qn = new FieldQueryNode();
        }
        qn.fromMap(m);
        return qn;
    }

    /**
     *
     * @return
     */
    public Map<String,Object> toMap(){
        Map<String,Object> m = Objects.newHashMap(String.class, Object.class,
                "not", not);

        return m;
    }

    /**
     *
     */
    public void fromMap(Map<String,Object> m){
        not = Objects.get(Boolean.class,m,"not",false);
    }

    /**
     *
     * @return
     */
    public boolean isNot() {
        return not;
    }

    /**
     *
     * @param not
     */
    public void setNot(boolean not) {
        this.not = not;
    }
}
