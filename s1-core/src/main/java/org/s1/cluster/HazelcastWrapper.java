package org.s1.cluster;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * Hazelcast wrapper, helps you to build hazelcast config, xml find order:
 * <ol>
 * <li>'HazelcastConfig' options parameter - defines full path to xml config file</li>
 * <li>Looks in default for current OptionsStorage place for config with name 'hazelcast.xml'</li>
 * <li>Instantiate default Config</li>
 * </ol>
 */
public class HazelcastWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(HazelcastWrapper.class);

    private static HazelcastInstance hazelcastInstance = null;

    public static final String XML_CONFIG_FILE_PATH = "hazelcastXMLConfigFilePath";
    public static final String DEFAULT_FILENAME = "Hazelcast.xml";

    public static synchronized HazelcastInstance getInstance(){
        if(hazelcastInstance!=null && !hazelcastInstance.getLifecycleService().isRunning())
            hazelcastInstance = null;
        if(hazelcastInstance==null){
            //HazecastConfig
            Config cfg = null;
            String hzConfigFile = Options.getStorage().getSystem(XML_CONFIG_FILE_PATH);
            try{
                if(!Objects.isNullOrEmpty(hzConfigFile)){
                    cfg = new XmlConfigBuilder(hzConfigFile).build();
                }
            }catch (Exception e){

            }
            if(cfg==null){
                //try config/hazelcast
                try{
                    cfg = (Config)Options.getStorage().readConfig(DEFAULT_FILENAME,new Closure<InputStream, Object>() {
                        @Override
                        public Object call(InputStream input) {
                            if(input!=null){
                                return new XmlConfigBuilder(input).build();
                            }
                            return null;
                        }
                    });
                }catch (ClosureException e){
                    throw e.toSystemError();
                }
            }
            if(cfg==null){
                if(LOG.isDebugEnabled())
                    LOG.info("Hazelcast config not found in '" + XML_CONFIG_FILE_PATH + "' System property nor in '" + DEFAULT_FILENAME + "' options config resource");
                new Config();
            }
            hazelcastInstance = Hazelcast.newHazelcastInstance(cfg);
            LOG.info("Hazelcast started, config: "+cfg);
        }
        return hazelcastInstance;
    }

}
