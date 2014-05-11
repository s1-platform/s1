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

package org.s1.table;

import org.s1.S1SystemError;
import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.s1.options.Options;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Table Factory
 */
public class TableFactory {

    private static Map<String,Table> cache = new ConcurrentHashMap<String, Table>();

    public Table get(final String name){
        if(!cache.containsKey(name)){
            Table wo = null;

            Map<String,Object> cls = Objects.find((List<Map<String,Object>>) Options.getStorage().getSystem(List.class, "tables.list"), new Closure<Map<String, Object>, Boolean>() {
                @Override
                public Boolean call(Map<String, Object> input) {
                    return name.equals(Objects.get(input, "name"));
                }
            });
            if(!Objects.isNullOrEmpty(cls)){
                try{
                    wo = (Table)Class.forName(Objects.get(String.class,cls,"class")).newInstance();
                    wo.init();
                }catch (Exception e){
                    throw new S1SystemError("Cannot initialize Table ("+cls+"): "+e.getMessage(),e);
                }
            }
            if(wo == null)
                throw new S1SystemError("Table "+name+" not found");
            cache.put(name,wo);
        }
        return cache.get(name);
    }

}
