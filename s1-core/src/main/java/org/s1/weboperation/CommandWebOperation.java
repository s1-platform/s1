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

package org.s1.weboperation;

import org.s1.objects.Objects;
import org.s1.objects.schema.ListAttribute;
import org.s1.objects.schema.MapAttribute;
import org.s1.objects.schema.ObjectSchema;
import org.s1.objects.schema.SimpleTypeAttribute;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Operation that executes multiple other web operations at once.<br>
 * Accepts requests like:
 * <pre>{list: [{
 *     operation: "...operation name...",
 *     method: "...method...",
 *     params: Object
 * }, ...]}</pre>
 * Usually uses with {@link org.s1.weboperation.MapWebOperation} because params must be parseable
 * with {@link org.s1.weboperation.MapWebOperation#convertRequestToMap(javax.servlet.http.HttpServletRequest)}
 */
public class CommandWebOperation extends MapWebOperation {

    @Override
    protected Map<String, Object> process(String method, Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        params = new ObjectSchema(
                new ListAttribute("list","list",new MapAttribute(null,null,
                        new SimpleTypeAttribute("operation","operation",String.class).setRequired(true),
                        new SimpleTypeAttribute("method","method",String.class),
                        new MapAttribute("params","params").setRequired(true).setDefault(Objects.newHashMap())
                ))
        ).validate(params);

        List<Map<String,Object>> listParams = Objects.get(params,"list");
        List<Map<String,Object>> listResults = Objects.newArrayList();

        for (Map<String,Object> cmd : listParams) {
            Map<String,Object> cmdFinalResult = null;
            try {
                Object cmdParams = Objects.get(cmd,"params");
                String cmdOperation = Objects.get(cmd,"operation");
                String cmdMethod = Objects.get(cmd,"method");
                WebOperation wo = DispatcherServlet.getOperationByName(cmdOperation);

                Object cmdResult = wo.process(cmdMethod, cmdParams, request, response);
                cmdFinalResult = Objects.newHashMap("data",cmdResult,"success",true);
            } catch (Throwable e) {
                logError(e);
                cmdFinalResult = Objects.newHashMap("data",transformError(e, request, response),"success",false);
            }
            listResults.add(cmdFinalResult);
        }

        return Objects.newHashMap("list",listResults);
    }
}
