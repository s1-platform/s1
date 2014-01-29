package org.s1.options;

import org.s1.objects.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Options
 */
public class Options {

    private static final Logger LOG = LoggerFactory.getLogger(OptionsStorage.class);

    private static OptionsStorage storage;

    /**
     *
     * @return
     */
    public static synchronized OptionsStorage getStorage() {
        if(storage==null){
            String cls = getParameter("OptionsStorage");

            try {
                if(!Objects.isNullOrEmpty(cls))
                    storage = (OptionsStorage)Class.forName(cls).newInstance();
            } catch (Exception e) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Cannot instantiate OptionsStorage class "+cls+": "+e.getMessage());
            }
            if(storage==null)
                storage = new OptionsStorage();
            LOG.info("OptionsStorage: ("+cls+") initialized");
        }
        return storage;
    }

    /**
     *
     * @param storage
     */
    public static synchronized void setStorage(OptionsStorage storage) {
        Options.storage = storage;
    }

    /**
     *
     * @param name
     * @return
     */
    static String getParameter(String name){
        String r = null;
        //then try system property
        if(!Objects.isNullOrEmpty(System.getProperty("s1."+name)))
            r = System.getProperty("s1."+name);
        //then try jndi
        if(r==null){
            try {
                Context initCtx = new InitialContext();
                Context envCtx = (Context) initCtx.lookup("java:comp/env");
                r = (String) envCtx.lookup("s1."+name);
            } catch (Exception e) {
            }
        }
        if(LOG.isDebugEnabled())
            LOG.debug("Read parameter "+name+": "+r);
        return r;
    }

}
