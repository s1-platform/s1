package org.s1.cluster.node;

import com.hazelcast.core.Member;
import org.s1.S1SystemError;
import org.s1.cluster.HazelcastWrapper;
import org.s1.log.LogStorage;
import org.s1.log.Loggers;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cluster node monitor server and client
 */
public class NodeMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(NodeMonitor.class);

    /**
     *
     * @return
     */
    private static int getPortMax(){
        int port = Options.getStorage().getSystem("cluster.monitorPortMax", 0);
        if(port<=0 || port>=65535){
            port = 22030;
        }
        return port;
    }

    /**
     *
     * @param portMax
     * @return
     */
    private static int getPortMin(int portMax){
        int port = Options.getStorage().getSystem("cluster.monitorPortMin", 0);
        if(port<=0 || port>=65535){
            port = 22020;
        }
        if(port>portMax)
            port = portMax;
        return port;
    }

    /**
     *
     * @param nodeId
     * @param threads
     * @param address
     */
    NodeMonitor(String nodeId, int threads, String address){
        this.nodeId = nodeId;
        if(threads<=0)
            threads = 10;
        this.threads = threads;
        this.portMax = getPortMax();
        this.portMin = getPortMin(portMax);
        this.address = address;
    }

    private String nodeId;
    private String address;
    private int portMin;
    private int portMax;
    private ExecutorService executor;
    private int threads;
    private volatile boolean run = false;
    private volatile String status = "stopped";
    private ServerSocket serverSocket;

    /**
     *
     * @return
     */
    Map<String,Object> getStatistic(){
        Map<String,Object> m = Objects.newHashMap();
        m.put("status",status);
        if("started".equals(status)){
            m.put("port",serverSocket.getLocalPort());
            m.put("address",serverSocket.getInetAddress().getHostAddress());
            m.put("threads",threads);
        }
        return m;
    }

    /**
     * Start server
     */
    synchronized void start() throws BindException{
        if(status.equals("started"))
            return;
        long t = System.currentTimeMillis();

        //starting server socket
        int port = portMin;
        for(;port<=portMax;port++){
            try{
                if(Objects.isNullOrEmpty(address))
                    serverSocket = new ServerSocket(port);
                else
                    serverSocket = new ServerSocket(port,50,InetAddress.getByName(address));
                break;
            }catch(BindException e){
            }catch (Exception e){
                throw new IllegalStateException(e.getMessage(),e);
            }
        }
        if(serverSocket==null){
            throw new BindException("Cannot bind MonitorServer to port range ["+portMin+":"+portMax+"]");
        }

        run = true;

        executor = Executors.newFixedThreadPool(threads);

        new Thread(new MonitorServer()).start();

        status = "started";
        LOG.info("Node "+nodeId+" monitor server started ("+(System.currentTimeMillis()-t)+" ms.) on port "+port+" with "+threads+" threads");
    }

    /**
     * Stop file server
     */
    synchronized void stop(){
        if(status.equals("stopped"))
            return;
        long t = System.currentTimeMillis();
        run = false;

        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        status = "stopped";
        LOG.info("Node "+nodeId+" monitor server stopped ("+(System.currentTimeMillis()-t)+" ms.)");
    }

    /**
     *
     */
    private class MonitorServer implements Runnable {

        @Override
        public void run() {
            while(true){
                if(!run){
                    break;
                }
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();
                } catch (IOException e) {
                    if(!run) {
                        return;
                    }
                    throw S1SystemError.wrap(e);
                }
                executor.execute(new MonitorServerWorker(clientSocket));
            }
        }

    }

    private LogStorage storage;

    /**
     * Get log storage
     * @return
     */
    public synchronized LogStorage getLogStorage(){
        if(storage==null){
            String cls = Options.getStorage().getSystem("cluster.logStorageClass", LogStorage.class.getName());
            try{
                storage = (LogStorage)Class.forName(cls).newInstance();
            }catch (Exception e){
                throw S1SystemError.wrap(e);
            }
        }
        return storage;
    }

    /**
     * Params are validated in {@link org.s1.cluster.monitor.MonitorOperation}
     *
     * @param command
     * @param params
     * @return
     */
    private Map<String,Object> getData(String command, Map<String,Object> params){
        Map<String,Object> result = Objects.newHashMap();
        if("clusterInfo".equals(command)){
            result = ClusterNode.getStatistic(false);
        }else if("getLoggers".equals(command)){
            result = Loggers.getLogClasses();
        }else if("setLogLevel".equals(command)){
            String name = Objects.get(params,"name");
            String level = Objects.get(params,"level");
            Loggers.setLogLevel(name, level);
        }else if("nodeLogs".equals(command)){
            int skip = Objects.get(params,"skip");
            int max = Objects.get(params,"max");
            Map<String,Object> search = Objects.get(params,"search");

            List<Map<String,Object>> list = Objects.newArrayList();
            long count = getLogStorage().list(list,search,skip,max);
            result.put("count",count);
            result.put("list",list);
        }else if("nodeInfo".equals(command)){
            result = ClusterNode.getStatistic(true);
        }
        return result;
    }

    /**
     *
     */
    private class MonitorServerWorker implements Runnable {
        private Socket socket;
        public MonitorServerWorker(Socket s){
            socket = s;
        }
        @Override
        public void run() {
            MonitorRequestBean req = null;
            try{

                req = (MonitorRequestBean)new ObjectInputStream(socket.getInputStream()).readObject();
                if(req!=null){
                    final MonitorRequestBean request = req;
                    if(req.getNodeId()==null || req.getNodeId().equals(nodeId)){
                        if(LOG.isDebugEnabled())
                            LOG.debug("Node "+nodeId+" recieved new monitor request: "+req.toString());
                        Map<String,Object> result = getData(req.getCommand(),req.getParams());

                        MonitorResponseBean resp = new MonitorResponseBean(result,true,null);
                        new ObjectOutputStream(socket.getOutputStream()).writeObject(resp);
                        socket.getOutputStream().flush();
                        if(LOG.isDebugEnabled())
                            LOG.debug("Monitor response sended to requestor successfully");

                    }else{
                        throw new Exception("different node id");
                    }
                }
            } catch (Throwable e){
                if(LOG.isDebugEnabled())
                    LOG.debug("Error executing monitor request: "+req.toString()+" - "+e.getClass().getName()+": "+e.getMessage());
                try{
                    MonitorResponseBean resp = new MonitorResponseBean(null,false,e.getClass().getName()+": "+e.getMessage());
                    new ObjectOutputStream(socket.getOutputStream()).writeObject(resp);
                    socket.getOutputStream().flush();
                }catch (Throwable e1){
                }
            }
        }
    }

    /**
     * Request for monitor data from node
     *
     * @param nodeId
     * @param command
     * @param params
     * @return
     */
    public static Map<String,Object> getMonitorData(String nodeId, String command, Map<String,Object> params) {
        MonitorRequestBean request = new MonitorRequestBean(nodeId,command,params);

        //
        List<String> ip_addresses = Objects.newArrayList();
        for(Member it:HazelcastWrapper.getInstance().getCluster().getMembers()){
            String ip = it.getInetSocketAddress().getAddress().getHostAddress();
            if(!ip_addresses.contains(ip))
                ip_addresses.add(ip);
        }

        int portMax = getPortMax();
        int portMin = getPortMin(portMax);

        Map<String,Object> response = Objects.newHashMap();
        boolean done = false;

        List<Map<String,Object>> nodeResponses = Objects.newArrayList();

        for(String ip:ip_addresses){
            for(int port = portMin;port<portMax;port++){
                Socket socket = null;
                try{
                    socket = new Socket(ip,port);
                    OutputStream os = socket.getOutputStream();
                    InputStream is = socket.getInputStream();
                    //send request
                    new ObjectOutputStream(os).writeObject(request);
                    os.flush();

                    //wait for response
                    MonitorResponseBean resp = (MonitorResponseBean)new ObjectInputStream(is).readObject();
                    if(resp!=null && resp.isSuccess()){
                        if(LOG.isDebugEnabled())
                            LOG.debug("Monitor response recieved");
                        response = resp.getResult();
                        nodeResponses.add(response);
                        if(nodeId!=null)
                            done = true;
                    }else{
                        if(LOG.isDebugEnabled())
                            LOG.debug("Monitor data not avaliable: "+resp.getErrorMessage());
                    }
                }catch (Throwable t) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("Exception requesting monitor data "+t.getClass().getName()+": "+t.getMessage());
                } finally {
                    try{
                        socket.close();
                    }catch (Exception ex){}
                }
                if(done)
                    break;
            }
            if(done)
                break;
        }
        if(nodeId==null){
            response = Objects.newHashMap("nodes",nodeResponses);
        }
        return response;
    }

    /**
     *
     */
    private static class MonitorRequestBean implements Serializable{
        private String nodeId;
        private String command;
        private Map<String,Object> params;

        private MonitorRequestBean(String nodeId, String command, Map<String, Object> params) {
            this.nodeId = nodeId;
            this.command = command;
            this.params = params;
        }

        public String getNodeId() {
            return nodeId;
        }

        public String getCommand() {
            return command;
        }

        public Map<String, Object> getParams() {
            return params;
        }

        public String toString(){
            return "nodeId: "+nodeId+", command: "+command+", params: "+params;
        }
    }

    /**
     *
     */
    private static class MonitorResponseBean implements Serializable{
        private Map<String,Object> result;
        private boolean success;
        private String errorMessage;

        private MonitorResponseBean(Map<String, Object> result, boolean success, String errorMessage) {
            this.result = result;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public Map<String, Object> getResult() {
            return result;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String toString(){
            String s = "success: "+success;
            if(success)
                s+=", result: "+result;
            else
                s+=", errorMessage: "+errorMessage;
            return s;
        }
    }

}
