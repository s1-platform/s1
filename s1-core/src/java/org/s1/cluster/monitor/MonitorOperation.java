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

import org.s1.cluster.HazelcastWrapper;
import org.s1.cluster.NodeMessageExchange;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.s1.weboperation.MapWebOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Cluster monitoring
 */
public class MonitorOperation extends MapWebOperation {

    /**
     *
     * @return
     * @throws TimeoutException
     */
    public static List<Map<String,Object>> getClusterInfo() throws TimeoutException {
        List<Map<String,Object>> l = Objects.newArrayList();
        if(NodeMessageExchange.getInstance()!=null){
            List<Object> lo = NodeMessageExchange.getInstance().multicast("monitor.getClusterInfo",null);
            for(Object o:lo){
                l.add((Map<String,Object>)o);
            }
        }
        return l;
    }

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
            Map<String,String> env = Objects.newHashMap();
            for(Map.Entry<String,String> e:System.getenv().entrySet()){
                env.put(""+e.getKey(),""+e.getValue());
            }
            m.put("env",env);
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
            result.put("nodes",getClusterInfo());
        }else if("getNodeInfo".equals(method)){
            String nodeId = Objects.get(params,"nodeId");
            Objects.assertNotEmpty("NodeId must not be empty",nodeId);
            result = (Map<String,Object>)NodeMessageExchange.getInstance().request(nodeId,"monitor.getNodeInfo",null);
        }else if("listNodeLogs".equals(method)){
            String nodeId = Objects.get(params,"nodeId");
            int skip = Objects.get(Integer.class,params,"skip",0);
            int max = Objects.get(Integer.class,params,"max",10);
            Map<String,Object> search = Objects.get(params,"search",Objects.newSOHashMap());
            Objects.assertNotEmpty("NodeId must not be empty",nodeId);

            result = (Map<String,Object>)NodeMessageExchange.getInstance().request(nodeId,"monitor.listNodeLogs",
                    Objects.newHashMap(
                            "search",search,
                            "skip",skip,
                            "max",max));
        }else if("setLogLevel".equals(method)){
            String nodeId = Objects.get(params,"nodeId");
            String name = Objects.get(params,"name");
            String level = Objects.get(params,"level");
            Objects.assertNotEmpty("NodeId must not be empty",nodeId);
            Objects.assertNotEmpty("Level must not be empty",level);

            NodeMessageExchange.getInstance().request(nodeId,"monitor.setLogLevel",
                    Objects.newHashMap(
                            "name",name,
                            "level",level));
        }else if("getLoggers".equals(method)){
            String nodeId = Objects.get(params,"nodeId");
            Objects.assertNotEmpty("NodeId must not be empty",nodeId);
            result = (Map<String,Object>)NodeMessageExchange.getInstance().request(nodeId,"monitor.getLoggers",null);
        }else{
            throwMethodNotFound(method);
        }
        return result;
    }
}
