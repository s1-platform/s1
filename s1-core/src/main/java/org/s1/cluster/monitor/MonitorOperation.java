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

package org.s1.cluster.monitor;

import org.s1.cluster.ClusterLifecycleAction;
import org.s1.cluster.HazelcastWrapper;
import org.s1.cluster.NodeMessageExchange;
import org.s1.cluster.Session;
import org.s1.objects.Objects;
import org.s1.objects.schema.MapAttribute;
import org.s1.objects.schema.ObjectSchema;
import org.s1.objects.schema.SimpleTypeAttribute;
import org.s1.options.Options;
import org.s1.script.S1ScriptEngine;
import org.s1.user.AuthException;
import org.s1.weboperation.MapWebOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Cluster monitoring
 */
public class MonitorOperation extends MapWebOperation {

    /**
     *
     * @return
     */
    public static Map<String,Object> getStatistic(boolean full){
        Map<String,Object> m = Objects.newHashMap();
        m.put("currentTimeMillis",System.currentTimeMillis());

        m.put("availableProcessors",Runtime.getRuntime().availableProcessors());
        m.put("freeMemory",Runtime.getRuntime().freeMemory());
        m.put("maxMemory",Runtime.getRuntime().maxMemory());
        m.put("totalMemory",Runtime.getRuntime().totalMemory());

        m.put("nodeId", Options.getStorage().getSystem("cluster.nodeId"));
        m.put("address", HazelcastWrapper.getInstance().getCluster().getLocalMember().getInetSocketAddress().getHostName());

        if(full){
            m.put("env",System.getenv());
            m.put("properties",System.getProperties());
            List<Map> roots = Objects.newArrayList();
            for(File f:File.listRoots()){
                roots.add(Objects.newHashMap(
                        "path",f.getAbsolutePath(),
                        "totalSpace",f.getTotalSpace(),
                        "freeSpace",f.getFreeSpace(),
                        "usableSpace",f.getUsableSpace()
                ));
            }
            m.put("fileSystemRoots", roots);
        }
        return m;
    }

    private static final Logger LOG = LoggerFactory.getLogger(MonitorOperation.class);

    @Override
    protected Map<String, Object> process(String method, Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String,Object> result = Objects.newHashMap();

        if("getClusterInfo".equals(method)){
            result.put("nodes",ClusterLifecycleAction.getNodeMessageExchange().multicast("monitor.getClusterInfo",null));
        }else if("getNodeInfo".equals(method)){
            params = new ObjectSchema(new SimpleTypeAttribute("nodeId","nodeId",String.class).setRequired(true))
                    .validate(params);
            String nodeId = Objects.get(params,"nodeId");
            result = (Map<String,Object>)ClusterLifecycleAction.getNodeMessageExchange().request(nodeId,"monitor.getNodeInfo",null);
        }else if("listNodeLogs".equals(method)){
            params = new ObjectSchema(new SimpleTypeAttribute("nodeId","nodeId",String.class).setRequired(true),
                    new SimpleTypeAttribute("skip","skip",Integer.class).setRequired(true).setDefault(0),
                    new SimpleTypeAttribute("max","max",Integer.class).setRequired(true).setDefault(10),
                    new MapAttribute("search","search")).validate(params);
            String nodeId = Objects.get(params,"nodeId");
            result = (Map<String,Object>)ClusterLifecycleAction.getNodeMessageExchange().request(nodeId,"monitor.listNodeLogs",
                    (Serializable)Objects.newHashMap(
                            "search",Objects.get(params,"search"),
                            "skip",Objects.get(params,"skip"),
                            "max",Objects.get(params,"max")));
        }else if("setLogLevel".equals(method)){
            params = new ObjectSchema(new SimpleTypeAttribute("nodeId","nodeId",String.class).setRequired(true),
                    new SimpleTypeAttribute("name","name",String.class),
                    new SimpleTypeAttribute("level","level",String.class).setRequired(true)
            ).validate(params);
            String nodeId = Objects.get(params,"nodeId");
            result = (Map<String,Object>)ClusterLifecycleAction.getNodeMessageExchange().request(nodeId,"monitor.setLogLevel",
                    (Serializable)Objects.newHashMap(
                            "name",Objects.get(params,"name"),
                            "level",Objects.get(params,"level")));
        }else if("getLoggers".equals(method)){
            params = new ObjectSchema(new SimpleTypeAttribute("nodeId","nodeId",String.class).setRequired(true)
            ).validate(params);
            String nodeId = Objects.get(params,"nodeId");
            result = (Map<String,Object>)ClusterLifecycleAction.getNodeMessageExchange().request(nodeId,"monitor.getLoggers",null);
        }else{
            throwMethodNotFound(method);
        }
        return result;
    }
}
