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

import org.s1.objects.BadDataException;
import org.s1.objects.MapMethodWrapper;
import org.s1.objects.Objects;
import org.s1.script.function.ScriptFunctionSet;
import org.s1.table.errors.*;
import org.s1.table.format.FieldsMask;
import org.s1.table.format.Query;
import org.s1.table.format.Sort;
import org.s1.user.AccessDeniedException;

import java.util.List;
import java.util.Map;

/**
 *
 */
public class TableScriptFunctions extends ScriptFunctionSet {

    /**
     *
     * @param table
     * @return
     */
    protected Table getTable(String table){
        return Tables.get(table);
    }

    @Override
    public Object callFunction(String method, List<Object> args) throws Exception {
        String table = (String)args.get(0);
        args = args.subList(1,args.size());
        return MapMethodWrapper.findAndInvoke(getTable(table), method, args);
    }

}
