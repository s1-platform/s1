package org.s1.weboperation;

import com.hazelcast.core.Hazelcast;
import org.s1.S1SystemError;
import org.s1.cluster.node.ClusterNode;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.misc.protocols.Init;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * s1v2
 * User: GPykhov
 * Date: 11.01.14
 * Time: 13:03
 */
@MultipartConfig
public class DispatcherServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(DispatcherServlet.class);

    @Override
    public void init() throws ServletException {
        super.init();
        Init.init();
        try{
            ClusterNode.start();
        }catch (Exception e){
            LOG.error("Cannot start ClusterNode: "+e.getMessage(),e);
            throw S1SystemError.wrap(e);
        }
    }

    @Override
    public void destroy() {
        ClusterNode.stop();
        Hazelcast.shutdownAll();
        super.destroy();
    }

    /**
     *
     * @param request
     * @param response
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     */
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        process(request, response);
    }

    /**
     *
     * @param request
     * @param response
     * @throws javax.servlet.ServletException
     * @throws IOException
     */
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
        process(request, response);
    }

    /**
     * Get method from request.
     * @param request
     * @return
     */
    protected String getMethod(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String q = uri.substring((request.getContextPath()+request.getServletPath()).length()+1);
        int i = q.indexOf("/");
        int j = q.indexOf(".");
        String method = null;
        if(Math.max(i,j)>0){
            int k = Math.min(i,j);
            if(k<0)
                k = Math.max(i,j);
            method = q.substring(k+1);
        }
        return method;
    }

    private static Map<String,WebOperation> cache = new ConcurrentHashMap<String, WebOperation>();

    /**
     * Override this method to add new weboperations or override existing
     * @param name
     * @return
     */
    public static WebOperation getOperationByName(final String name) throws WebOperationNotFoundException{
        if(!cache.containsKey(name)){
            WebOperation wo = null;

            Map<String,Object> cls = Objects.find((List<Map<String,Object>>)Options.getStorage().getSystem(List.class,"webOperations"),new Closure<Map<String, Object>, Boolean>() {
                @Override
                public Boolean call(Map<String, Object> input) throws ClosureException {
                    return name.equals(Objects.get(input, "name"));
                }
            });
            if(!Objects.isNullOrEmpty(cls)){
                try{
                    wo = (WebOperation)Class.forName(Objects.get(String.class,cls,"class")).newInstance();
                    wo.setConfig(Objects.get(cls, "config", Objects.newHashMap(String.class, Object.class)));
                }catch (Exception e){
                    throw new WebOperationNotFoundException("Cannot initialize WebOperation ("+cls+"): "+e.getMessage(),e);
                }
            }
            if(wo == null)
                throw new WebOperationNotFoundException("WebOperation "+name+" not found");
            cache.put(name,wo);
        }
        return cache.get(name);
    }

    /**
     * Get operation from request
     * @param request
     * @return
     */
    protected WebOperation getOperation(HttpServletRequest request) throws WebOperationNotFoundException{
        String uri = request.getRequestURI();
        String q = uri.substring((request.getContextPath()+request.getServletPath()).length()+1);
        int i = q.indexOf("/");
        int j = q.indexOf(".");
        String name = q;
        if(Math.max(i,j)>0){
            int k = Math.min(i,j);
            if(k<0)
                k = Math.max(i,j);
            name = q.substring(0,k);
        }
        return getOperationByName(name);
    }

    /**
     * Process GET|POST request
     * @param request
     * @param response
     * @throws javax.servlet.ServletException
     * @throws IOException
     */
    protected void process(HttpServletRequest request,
                           HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        WebOperation operation = null;
        try{
            operation = getOperation(request);
        }catch (WebOperationNotFoundException e){
            LOG.warn("WebOperation found problems: "+e.getMessage(),e);
            response.setStatus(404);
            return;
        }

        String method = getMethod(request);
        operation.request(method, request, response);
    }

}
