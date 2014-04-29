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

package org.s1.cluster;

import com.hazelcast.core.IMap;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.Serializable;
import java.util.Map;

/**
 * Distributed Session to store current user, and some other data <br>
 * Session is identified by id. Client identifies his requests with this id.<br>
 * Session data is ThreadLocal - so if you initialize session you can access it from every place of current thread.
 * After processing all ThreadLocal variable would clear. <br>
 * While session is running it puts into slf4j MDC its id called 'sessionId'.
 */
public class Session {

    private static final Logger LOG = LoggerFactory.getLogger(Session.class);

    private static final ThreadLocal<String> idLocal = new ThreadLocal<String>();

    /**
     *
     */
    public static final String SESSION_MAP = "SessionMap";

    private static IMap<String,SessionBean> sessionMap;
    private static long TTL = 0;

    public static void destroy(){
        sessionMap = null;
    }

    /**
     *
     * @return
     */
    private static synchronized IMap<String,SessionBean> getSessionMap(){
        if(sessionMap==null){
            sessionMap = HazelcastWrapper.getInstance().getMap(SESSION_MAP);
        }
        if(TTL<=0){
            TTL = Options.getStorage().getSystem("session.ttl", 30 * 60 * 1000L);
        }

        return sessionMap;
    }

    /**
     * Run some code in session
     *
     * @param id session id
     * @return
     */
    public static String start(String id) {
        if(idLocal.get()!=null){
            return null;
        }else{
            MDC.put("sessionId", id);
            if(LOG.isDebugEnabled())
                LOG.debug("Initialize session id = "+id);
            idLocal.set(id);
            return id;
        }
    }

    /**
     *
     * @param id
     */
    public static void end(String id) {
        if(!Objects.isNullOrEmpty(id)){
            MDC.remove("sessionId");
            idLocal.remove();
        }
    }

    /**
     * Return current session data
     *
     * @return
     */
    public static SessionBean getSessionBean(){
        String id = idLocal.get();
        if(id==null)
            return null;
        SessionBean s = getSessionMap().get(id);

        //check TTL
        if(s!=null && (System.currentTimeMillis()-s.lastUsed)>TTL){
            if(LOG.isDebugEnabled())
                LOG.debug("Discarding session "+id+" with livetime="+(System.currentTimeMillis()-s.lastUsed)+"ms");
            s = null;
        }

        //new session
        if(s==null)
            s = new SessionBean();

        s.lastUsed = System.currentTimeMillis();
        updateSessionBean(s);
        s.id = id;
        return s;
    }

    /**
     *
     * @param s
     */
    private static void updateSessionBean(SessionBean s){
        String id = idLocal.get();
        if(id==null)
            return;
        getSessionMap().put(id, s);
    }

    /**
     * Anonymous user id
     */
    public static final String ANONYMOUS = "anonymous";

    /**
     * Root user id
     */
    public static final String ROOT = "root";

    /**
     * Session data
     */
    public static class SessionBean implements Serializable{
        private String id;
        private long created = System.currentTimeMillis();
        private long lastUsed = System.currentTimeMillis();
        private String userId = ANONYMOUS;
        private Map<String,Object> data = Objects.newHashMap();

        /**
         * Session id
         *
         * @return
         */
        public String getId() {
            return id;
        }

        /**
         * Create timestamp
         *
         * @return
         */
        public long getCreated() {
            return created;
        }

        /**
         * Last used timestamp
         *
         * @return
         */
        public long getLastUsed() {
            return lastUsed;
        }

        /**
         * User id
         *
         * @return
         */
        public String getUserId() {
            if(userId==null)
                return ANONYMOUS;
            return userId;
        }

        /**
         *
         * @param userId
         */
        public void setUserId(String userId) {
            this.userId = userId;
            updateSessionBean(this);
        }

        /**
         * Get session parameter
         *
         * @param path
         * @param <T>
         * @return
         */
        public <T> T get(String path){
            return get(path,null);
        }

        /**
         *
         * @param path
         * @param def
         * @param <T>
         * @return
         */
        public <T> T get(String path, T def){
            return Objects.get(data,path,def);
        }

        /**
         *
         * @param cls
         * @param path
         * @param <T>
         * @return
         */
        public <T> T get(Class<T> cls, String path){
            return get(cls,path,null);
        }

        /**
         *
         * @param cls
         * @param path
         * @param def
         * @param <T>
         * @return
         */
        public <T> T get(Class<T> cls,String path, T def){
            return Objects.get(cls,data,path,def);
        }

        /**
         * Set session parameter
         *
         * @param path
         * @param val
         */
        public void set(String path, Object val){
            Objects.set(data,path,val);
            updateSessionBean(this);
        }

    }

}
