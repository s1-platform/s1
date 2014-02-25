package org.s1.user;

import org.s1.S1SystemError;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * User helper class
 */
public class Users {

    private static final Logger LOG = LoggerFactory.getLogger(Users.class);

    private static volatile UserFactory userFactory;

    private static synchronized UserFactory getUserFactory(){
        if(userFactory==null){
            String cls = Options.getStorage().getSystem("users.factoryClass",UserFactory.class.getName());
            try{
                userFactory = (UserFactory) Class.forName(cls).newInstance();
            }catch (Throwable e){
                LOG.warn("Cannot initialize UserFactory ("+cls+") :"+e.getClass().getName()+": "+e.getMessage());
                throw S1SystemError.wrap(e);
            }
        }
        return userFactory;
    }

    /**
     *
     * @param id
     * @return
     * @throws NotFoundException
     */
    public static UserBean getUser(String id) throws NotFoundException{
        return getUserFactory().getUser(id);
    }

    /**
     *
     * @param user
     * @param role
     * @return
     */
    public static boolean isUserInRole(UserBean user, String role){
        return getUserFactory().isUserInRole(user,role);
    }

}
