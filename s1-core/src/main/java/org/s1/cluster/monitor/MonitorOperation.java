package org.s1.cluster.monitor;

import org.s1.cluster.Session;
import org.s1.cluster.node.NodeMonitor;
import org.s1.log.Loggers;
import org.s1.objects.Objects;
import org.s1.objects.schema.MapAttribute;
import org.s1.objects.schema.ObjectSchema;
import org.s1.objects.schema.SimpleTypeAttribute;
import org.s1.script.S1ScriptEngine;
import org.s1.user.AuthException;
import org.s1.weboperation.MapWebOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Cluster monitoring
 */
public class MonitorOperation extends MapWebOperation {

    private static final Logger LOG = LoggerFactory.getLogger(MonitorOperation.class);

    @Override
    protected Map<String, Object> process(String method, Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        checkAccess();
        Map<String,Object> result = Objects.newHashMap();
        if("clusterInfo".equals(method)){
            result = NodeMonitor.getMonitorData(null,method,params);
        }else if("nodeInfo".equals(method)){
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
            result = NodeMonitor.getMonitorData(nodeId, method, params);
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
        String userId = Session.getSessionBean().getUserId();
        boolean ok = false;
        if(Session.ROOT.equals(userId))
            ok = true;
        String s = Objects.get(config,"accessScript");
        if(!Objects.isNullOrEmpty(s)){
            try{
                ok = new S1ScriptEngine().evalInFunction(Boolean.class,s,Objects.newHashMap(String.class,Object.class,"userId",userId));
            }catch (Throwable e){
                if(LOG.isDebugEnabled())
                    LOG.debug("Access script error: "+e.getMessage(),e);
            }
        }
        if(!ok)
            throw new AuthException(userId+" has no rights to use cluster monitor");
    }

}
