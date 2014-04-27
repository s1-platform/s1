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

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.s1.misc.Closure;
import org.s1.misc.IOUtils;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * Hazelcast wrapper, helps you to build hazelcast config, xml find order:
 * <ol>
 * <li>'HazelcastConfig' options parameter - defines full path to xml config file</li>
 * <li>Looks in default for current OptionsStorage place for config with name 'Hazelcast.xml'</li>
 * <li>Instantiate default Config</li>
 * </ol>
 */
public class HazelcastWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(HazelcastWrapper.class);

    private static HazelcastInstance hazelcastInstance = null;

    /**
     * Default options path 'hazelcastXMLConfigFilePath'
     */
    public static final String XML_CONFIG_FILE_PATH = "hazelcastXMLConfigFilePath";

    /**
     * Hazelcast.xml
     */
    public static final String DEFAULT_FILENAME = "Hazelcast.xml";

    /**
     *
     * @return
     */
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
                InputStream is = null;
                try{
                    is = Options.getStorage().openConfig(DEFAULT_FILENAME);
                    cfg = new XmlConfigBuilder(is).build();
                }finally {
                    IOUtils.closeQuietly(is);
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
