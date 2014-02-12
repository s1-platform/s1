package org.s1.log;

import org.s1.S1SystemError;
import org.s1.user.AccessDeniedException;
import org.s1.cluster.Session;
import org.s1.objects.Objects;
import org.s1.objects.schema.ObjectSchema;
import org.s1.objects.schema.SimpleTypeAttribute;
import org.s1.weboperation.MapWebOperation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * WebOperation for managing log. Requires user id == 'root'
 */
public class LogOperation extends MapWebOperation {

    @Override
    protected Map<String, Object> process(String method, Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String,Object> res = Objects.newHashMap();
        checkAccess();
        if("get".equals(method)){
            res = Loggers.getLogClasses();
        }else if("set".equals(method)){
            params = new ObjectSchema(
                    new SimpleTypeAttribute("name","name",String.class),
                    new SimpleTypeAttribute("level","level",String.class).setRequired(true)
            ).validate(params);
            Loggers.setLogLevel(Objects.get(String.class,params,"name"),Objects.get(String.class,params,"level"));
        }else if("list".equals(method)){
            List<Map<String,Object>> l = Objects.newArrayList();
            long c = getStorage().list(l,null,Objects.get(params,"skip",0),Objects.get(params,"max",10));
            res = Objects.newHashMap("count",c,"list",l);
        }else{
            throwMethodNotFound(method);
        }
        return res;
    }

    /**
     *
     * @throws AccessDeniedException
     */
    protected void checkAccess() throws AccessDeniedException{
        if(!Session.getSessionBean().getUserId().equals("root"))
            throw new AccessDeniedException("You must have root access");

    }

    private LogStorage storage;

    /**
     * Get log storage
     * @return
     */
    public synchronized LogStorage getStorage(){
        if(storage==null){
            String cls = Objects.get(config,"storageClass",LogStorage.class.getName());
            try{
                storage = (LogStorage)Class.forName(cls).newInstance();
            }catch (Exception e){
                throw S1SystemError.wrap(e);
            }
        }
        return storage;
    }
}
