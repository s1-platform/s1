package org.s1.cluster.node;

import com.hazelcast.core.Member;
import org.s1.S1SystemError;
import org.s1.cluster.HazelcastWrapper;
import org.s1.cluster.datasource.FileStorage;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.misc.IOUtils;
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
 * Cluster file exchange server and client
 */
public class NodeFileExchange {
    private static final Logger LOG = LoggerFactory.getLogger(NodeFileExchange.class);

    /**
     *
     * @return
     */
    private static int getPortMax(){
        int port = Options.getStorage().getSystem("cluster.filePortMax", 0);
        if(port<=0 || port>=65535){
            port = 21030;
        }
        return port;
    }

    /**
     *
     * @param portMax
     * @return
     */
    private static int getPortMin(int portMax){
        int port = Options.getStorage().getSystem("cluster.filePortMin", 0);
        if(port<=0 || port>=65535){
            port = 21020;
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
    NodeFileExchange(String nodeId, int threads, String address){
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
            throw new BindException("Cannot bind FileServer to port range ["+portMin+":"+portMax+"]");
        }

        run = true;

        executor = Executors.newFixedThreadPool(threads);

        new Thread(new FileServer()).start();

        status = "started";
        LOG.info("Node "+nodeId+" file server started ("+(System.currentTimeMillis()-t)+" ms.) on port "+port+" with "+threads+" threads");
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
        LOG.info("Node "+nodeId+" file server stopped ("+(System.currentTimeMillis()-t)+" ms.)");
    }

    /**
     *
     */
    private class FileServer implements Runnable {

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
                executor.execute(new FileServerWorker(clientSocket));
            }
        }

    }

    /**
     *
     */
    private class FileServerWorker implements Runnable {
        private Socket socket;
        public FileServerWorker(Socket s){
            socket = s;
        }
        @Override
        public void run() {
            FileRequestBean req = null;
            try{

                req = (FileRequestBean)new ObjectInputStream(socket.getInputStream()).readObject();
                if(req!=null){
                    final FileRequestBean request = req;
                    if(req.getNodeId().equals(nodeId))
                        throw new Exception("same node request");
                    if(LOG.isDebugEnabled())
                        LOG.debug("Node "+nodeId+" recieved new file request: "+req.toString());
                    FileStorage.read(req.getGroup(), req.getId(), new Closure<FileStorage.FileReadBean, Object>() {
                        @Override
                        public Object call(FileStorage.FileReadBean input) throws ClosureException{
                            try{
                            FileResponseBean resp = new FileResponseBean(nodeId, request.getId(), request.getGroup(), true, null, input.getMeta().getSize());
                            new ObjectOutputStream(socket.getOutputStream()).writeObject(resp);
                            IOUtils.copy(input.getInputStream(),socket.getOutputStream());
                            //sendMessage([success:true,group:req.group,id:req.id,nodeId:nodeId],meta.size,os);
                            //IOUtils.copy(fis, os);
                            if(LOG.isDebugEnabled())
                                LOG.debug("File (group: "+request.getGroup()+", id: "+request.getId()+", size: "+input.getMeta().getSize()+") sended to "+request.getNodeId()+" successfully");

                            }catch (Exception e){
                                LOG.warn("Error in FileServerWorker thread: "+e.getMessage(),e);
                                throw S1SystemError.wrap(e);
                            }
                            return null;
                        }
                    });
                }
            } catch (Throwable e){
                if(LOG.isDebugEnabled())
                    LOG.debug("Error getting file for request: "+req+" - "+e.getClass().getName()+": "+e.getMessage());
                try{
                    FileResponseBean resp = new FileResponseBean(nodeId,req.getId(),req.getGroup(),false,e.getMessage(),0);
                    new ObjectOutputStream(socket.getOutputStream()).writeObject(resp);
                }catch (Throwable e1){
                }
            }
        }
    }

    /**
     * Request for file from cluster
     *
     * @param group
     * @param id
     * @param cl
     */
    public static void getFile(String group, String id, Closure<GetFileBean,Object> cl) throws ClosureException {
        FileRequestBean request = new FileRequestBean(ClusterNode.getCurrentNodeId(),id,group);

        //download from fileserver
        List<String> ip_addresses = Objects.newArrayList();
        for(Member it:HazelcastWrapper.getInstance().getCluster().getMembers()){
            if(!it.localMember()){
                String ip = it.getInetSocketAddress().getAddress().getHostAddress();
                if(!ip_addresses.contains(ip))
                    ip_addresses.add(ip);
            }
        }

        int portMax = getPortMax();
        int portMin = getPortMin(portMax);

        boolean done = false;

        while(true){
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
                        FileResponseBean resp = (FileResponseBean)new ObjectInputStream(is).readObject();
                        if(resp!=null && resp.isSuccess()){
                            if(LOG.isDebugEnabled())
                                LOG.debug("File (group: "+resp.getGroup()+", id: "+resp.getId()+") recieved from node "+resp.getNodeId()+", size: "+resp.getSize()+", begin copying...");
                            long t = System.currentTimeMillis();
                            cl.call(new GetFileBean(is,resp.getSize()));
                            if(LOG.isDebugEnabled())
                                LOG.debug("File (group: "+resp.getGroup()+", id: "+resp.getId()+") recieved from node "+resp.getNodeId()+", size: "+resp.getSize()+", copied successfully ("+(System.currentTimeMillis()-t)+" ms.)");
                            done = true;
                        }else{
                            if(LOG.isDebugEnabled())
                                LOG.debug("File not avaliable on node "+resp.getNodeId()+": "+resp.getErrorMessage());
                        }
                    }catch (Throwable t) {
                        if(LOG.isDebugEnabled())
                            LOG.debug("Exception requesting file "+t.getClass().getName()+": "+t.getMessage());
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
            if(done)
                break;
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw S1SystemError.wrap(e);
                }
            }
    }

    /**
     *
     */
    private static class FileRequestBean implements Serializable{
        private String nodeId;
        private String id;
        private String group;

        private FileRequestBean(String nodeId, String id, String group) {
            this.nodeId = nodeId;
            this.id = id;
            this.group = group;
        }

        public String getNodeId() {
            return nodeId;
        }

        public String getId() {
            return id;
        }

        public String getGroup() {
            return group;
        }

        public String toString(){
            return "nodeId: "+nodeId+", group: "+group+", id: "+id;
        }
    }

    /**
     * Get file bean
     */
    public static class GetFileBean implements Serializable{
        private InputStream inputStream;
        private long size;

        private GetFileBean(InputStream inputStream, long size) {
            this.inputStream = inputStream;
            this.size = size;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public long getSize() {
            return size;
        }
    }

    /**
     *
     */
    private static class FileResponseBean implements Serializable{
        private String nodeId;
        private String id;
        private String group;
        private boolean success;
        private String errorMessage;
        private long size;

        private FileResponseBean(String nodeId, String id, String group, boolean success, String errorMessage, long size) {
            this.nodeId = nodeId;
            this.id = id;
            this.group = group;
            this.success = success;
            this.errorMessage = errorMessage;
            this.size = size;
        }

        public String getNodeId() {
            return nodeId;
        }

        public String getId() {
            return id;
        }

        public String getGroup() {
            return group;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public long getSize() {
            return size;
        }

        public String toString(){
            String s = "success: "+success+ ", nodeId: "+nodeId+", group: "+group+", id: "+id;
            if(success)
                s+=", size: "+size;
            else
                s+=", errorMessage: "+errorMessage;
            return s;
        }
    }

}
