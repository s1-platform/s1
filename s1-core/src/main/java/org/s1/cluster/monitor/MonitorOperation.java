package org.s1.cluster.monitor;

import org.s1.cluster.Session;
import org.s1.cluster.node.NodeMonitor;
import org.s1.log.Loggers;
import org.s1.objects.Objects;
import org.s1.objects.schema.MapAttribute;
import org.s1.objects.schema.ObjectSchema;
import org.s1.objects.schema.SimpleTypeAttribute;
import org.s1.user.AuthException;
import org.s1.weboperation.MapWebOperation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Cluster monitoring
 */
public class MonitorOperation extends MapWebOperation {

    @Override
    protected Map<String, Object> process(String method, Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        checkAccess();
        Map<String,Object> result = Objects.newHashMap();
        if("clusterInfo".equals(method)){
            List<Map<String,Object>> nodes = Objects.newArrayList();
            result.put("nodes",nodes);

        }else if("nodeIndicators".equals(method)){
            params = new ObjectSchema(new SimpleTypeAttribute("nodeId","nodeId",String.class).setRequired(true))
                    .validate(params);
            String nodeId = Objects.get(params,"nodeId");
            result = NodeMonitor.getMonitorData(nodeId,method,params);
        }else if("nodeLogs".equals(method)){
            params = new ObjectSchema(new SimpleTypeAttribute("nodeId","nodeId",String.class).setRequired(true),
                    new SimpleTypeAttribute("skip","skip",Integer.class).setRequired(true).setDefault(0),
                    new SimpleTypeAttribute("max","max",Integer.class).setRequired(true).setDefault(10),
                    new MapAttribute("search","search")).validate(params);
            String nodeId = Objects.get(params,"nodeId");
            result = NodeMonitor.getMonitorData(nodeId,method,params);
        }else if("setLogLevel".equals(method)){
            params = new ObjectSchema(new SimpleTypeAttribute("nodeId","nodeId",String.class).setRequired(true),
                    new SimpleTypeAttribute("name","name",String.class),
                    new SimpleTypeAttribute("level","level",String.class).setRequired(true)
            ).validate(params);
            String nodeId = Objects.get(params,"nodeId");
            result = NodeMonitor.getMonitorData(nodeId,method,params);
        }else if("getLoggers".equals(method)){
            params = new ObjectSchema(new SimpleTypeAttribute("nodeId","nodeId",String.class).setRequired(true)
            ).validate(params);
            String nodeId = Objects.get(params,"nodeId");
            result = NodeMonitor.getMonitorData(nodeId,method,params);
        }else{
            throwMethodNotFound(method);
        }
        return result;
    }

    /**
     *
     * @throws AuthException
     */
    protected void checkAccess() throws AuthException{
        if(Session.ROOT.equals(Session.getSessionBean().getUserId()))
            return;
        throw new AuthException(Session.getSessionBean().getUserId()+" has no rights to use cluster monitor");
    }

}
