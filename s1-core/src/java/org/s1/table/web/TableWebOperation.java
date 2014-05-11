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

package org.s1.table.web;

import org.s1.objects.MapMethodWrapper;
import org.s1.objects.Objects;
import org.s1.table.Table;
import org.s1.table.Tables;
import org.s1.weboperation.MapWebOperation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Table web operation
 */
public class TableWebOperation extends MapWebOperation{

    protected Table getTable(Map<String,Object> params){
        String t = Objects.get(params,"table");
        return Tables.get(t);
    }

    @Override
    protected Map<String, Object> process(String method, Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return asMap(MapMethodWrapper.findAndInvoke(getTable(params),method,params));
    }
}
