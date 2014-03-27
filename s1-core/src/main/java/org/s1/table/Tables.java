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
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shorthand for Table Factory
 */
public class Tables {

    private static final Logger LOG = LoggerFactory.getLogger(Tables.class);

    private static volatile TableFactory tableFactory;

    private static synchronized TableFactory getTableFactory(){
        if(tableFactory==null){
            String cls = Options.getStorage().getSystem("tables.factoryClass",TableFactory.class.getName());
            try{
                tableFactory = (TableFactory) Class.forName(cls).newInstance();
            }catch (Throwable e){
                LOG.warn("Cannot initialize TableFactory ("+cls+") :"+e.getClass().getName()+": "+e.getMessage());
                throw S1SystemError.wrap(e);
            }
        }
        return tableFactory;
    }

    public static Table get(String name){
        Table t = getTableFactory().get(name);
        if(t==null){
            throw new S1SystemError("Table "+name+" not found");
        }
        return t;
    }

}
