package org.s1.user;

import org.s1.S1SystemError;
import org.s1.cluster.Session;
import org.s1.log.LogStorage;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.s1.weboperation.MapWebOperation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Standard login/password authentication operation
 */
public class AuthOperation extends MapWebOperation {

    @Override
    protected Map<String, Object> process(String method, Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String,Object> res = Objects.newHashMap();

        if("login".equals(method)){
            Session.getSessionBean().setUserId(null);
            String name = Objects.get(params,"name","");
            String password = Objects.get(params,"password","");
            if(Session.ROOT.equals(name)){
                if(password.equals(Options.getStorage().getSystem("root"))){
                    Session.getSessionBean().setUserId(Session.ROOT);
                }
            }else{
                //find user
                String user = getStorage().auth(name,password);
                if(user!=null){
                    Session.getSessionBean().setUserId(user);
                }
            }
            if(Session.getSessionBean().getUserId().equals(Session.ANONYMOUS)){
                //error
                throw new AuthException("Incorrect username/password");
            }
        }else if("logout".equals(method)){
            Session.getSessionBean().setUserId(null);
        }else{
            throwMethodNotFound(method);
        }
        return res;
    }

    private AuthStorage storage;

    /**
     * Gets authentication data storage
     *
     * @return
     */
    public synchronized AuthStorage getStorage(){
        if(storage==null){
            String cls = Objects.get(config,"storageClass",LogStorage.class.getName());
            try{
                storage = (AuthStorage)Class.forName(cls).newInstance();
            }catch (Exception e){
                throw S1SystemError.wrap(e);
            }
        }
        return storage;
    }
}
