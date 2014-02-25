package org.s1.user;

import org.s1.cluster.datasource.NotFoundException;
import org.s1.objects.Objects;

import java.util.Map;

/**
 * Abstract user factory
 */
public class UserFactory {

    /**
     *
     * @param id
     * @return
     * @throws NotFoundException
     */
    public UserBean getUser(String id) throws NotFoundException {
        return new UserBean(Objects.newHashMap(String.class,Object.class,"id",id));
    }

    /**
     *
     * @param user
     * @param role
     * @return
     */
    public boolean isUserInRole(UserBean user, String role){
        return false;
    }

}
