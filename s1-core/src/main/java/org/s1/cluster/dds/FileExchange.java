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

package org.s1.cluster.dds;

import com.hazelcast.core.Member;
import org.s1.S1SystemError;
import org.s1.cluster.HazelcastWrapper;
import org.s1.cluster.dds.beans.Id;
import org.s1.cluster.dds.file.FileStorage;
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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cluster file exchange server and client
 */
public class FileExchange {
    private static final Logger LOG = LoggerFactory.getLogger(FileExchange.class);

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
    FileExchange(String nodeId, int threads, String address){
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

        executor = Executors.newFixedThreadPool(threads,new ThreadFactory() {
            private AtomicInteger i = new AtomicInteger(-1);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "FileExchangeThread-"+i.incrementAndGet());
            }
        });

        new Thread(new FileServer(),"FileExchangeServerThread").start();

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
            FileRequestBean request = null;
            try{

                request = (FileRequestBean)new ObjectInputStream(socket.getInputStream()).readObject();
                if(request!=null){
                    if(request.getNodeId().equals(nodeId))
                        throw new Exception("same node request");
                    if(LOG.isDebugEnabled())
                        LOG.debug("Node "+nodeId+" recieved new file request: "+request.toString());
                    FileStorage.FileReadBean b = null;
                    try{
                        b = FileStorage.read(request.getId());
                        FileResponseBean resp = new FileResponseBean(nodeId, request.getId(),true, null, b.getMeta().getSize());
                        new ObjectOutputStream(socket.getOutputStream()).writeObject(resp);
                        IOUtils.copy(b.getInputStream(), socket.getOutputStream());
                        if (LOG.isDebugEnabled())
                            LOG.debug("File (id: " + request.getId() + ", size: " + b.getMeta().getSize() + ") sended to " + request.getNodeId() + " successfully");

                    }finally {
                        FileStorage.closeAfterRead(b);
                    }
                }
            } catch (Throwable e){
                if(LOG.isDebugEnabled())
                    LOG.debug("Error getting file for request: "+request+" - "+e.getClass().getName()+": "+e.getMessage());
                try{
                    FileResponseBean resp = new FileResponseBean(nodeId,request.getId(),false,e.getMessage(),0);
                    new ObjectOutputStream(socket.getOutputStream()).writeObject(resp);
                }catch (Throwable e1){
                }
            }
        }
    }

    /**
     *
     * @param b
     */
    public static void closeAfterRead(GetFileBean b){
        try{
            b.socket.close();
        }catch (Throwable e){

        }
    }

    /**
     *
     * @param id
     * @return
     */
    public static GetFileBean getFile(Id id){
        FileRequestBean request = new FileRequestBean(DDSCluster.getCurrentNodeId(),id);
        GetFileBean result = null;

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
                                LOG.debug("File (id: "+resp.getId()+") recieved from node "+resp.getNodeId()+", size: "+resp.getSize()+", begin copying...");
                            long t = System.currentTimeMillis();
                            result = new GetFileBean(is,resp.getSize(),socket);
                            if(LOG.isDebugEnabled())
                                LOG.debug("File (id: "+resp.getId()+") recieved from node "+resp.getNodeId()+", size: "+resp.getSize()+", copied successfully ("+(System.currentTimeMillis()-t)+" ms.)");
                        }else{
                            if(LOG.isDebugEnabled())
                                LOG.debug("File not avaliable on node "+resp.getNodeId()+": "+resp.getErrorMessage());
                        }
                    }catch (Throwable t) {
                        if(LOG.isDebugEnabled())
                            LOG.debug("Exception requesting file "+t.getClass().getName()+": "+t.getMessage());
                    } finally {
                        if(result==null) {
                            try {
                                socket.close();
                            } catch (Exception ex) {
                            }
                        }
                    }
                    if(result!=null)
                        break;
                }
                if(result!=null)
                    break;
            }
            if(result!=null)
                break;
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw S1SystemError.wrap(e);
            }
        }
        return result;
    }

    /**
     *
     */
    private static class FileRequestBean implements Serializable{
        private String nodeId;
        private Id id;

        private FileRequestBean(String nodeId, Id id) {
            this.nodeId = nodeId;
            this.id = id;
        }

        public String getNodeId() {
            return nodeId;
        }

        public Id getId() {
            return id;
        }

        public String toString(){
            return "nodeId: "+nodeId+", id: "+id;
        }
    }

    /**
     * Get file bean
     */
    public static class GetFileBean implements Serializable{
        private InputStream inputStream;
        private long size;
        private Socket socket;

        private GetFileBean(InputStream inputStream, long size, Socket socket) {
            this.inputStream = inputStream;
            this.size = size;
            this.socket = socket;
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
        private Id id;
        private boolean success;
        private String errorMessage;
        private long size;

        private FileResponseBean(String nodeId, Id id, boolean success, String errorMessage, long size) {
            this.nodeId = nodeId;
            this.id = id;
            this.success = success;
            this.errorMessage = errorMessage;
            this.size = size;
        }

        public String getNodeId() {
            return nodeId;
        }

        public Id getId() {
            return id;
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
            String s = "success: "+success+ ", nodeId: "+nodeId+", id: "+id;
            if(success)
                s+=", size: "+size;
            else
                s+=", errorMessage: "+errorMessage;
            return s;
        }
    }

}
