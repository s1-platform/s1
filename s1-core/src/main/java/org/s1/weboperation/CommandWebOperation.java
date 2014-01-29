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
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 22:39
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
