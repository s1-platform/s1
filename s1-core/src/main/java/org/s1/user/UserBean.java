package org.s1.user;

import org.s1.objects.Objects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * User bean
 */
public class UserBean extends HashMap<String,Object>{

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String FULL_NAME = "fullName";

    public UserBean(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public UserBean(int initialCapacity) {
        super(initialCapacity);
    }

    public UserBean() {
        super();
    }

    public UserBean(Map<? extends String, ?> m) {
        super(m);
    }

    /**
     *
     * @return
     */
    public String getId() {
        return Objects.get(this,ID);
    }

    /**
     *
     * @return
     */
    public String getName() {
        return Objects.get(this,NAME);
    }

    /**
     *
     * @return
     */
    public String getFullName() {
        return Objects.get(this, FULL_NAME);
    }
}
