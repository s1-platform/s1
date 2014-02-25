package org.s1.table;

import org.s1.S1SystemError;
import org.s1.misc.IOUtils;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Tables factory
 */
public class TablesFactory {

    private static final Logger LOG = LoggerFactory.getLogger(TablesFactory.class);

    private static final Map<String, Table> local = Objects.newHashMap();

    /**
     *
     * @param name
     * @return
     */
    public static synchronized Table getTable(String name){
        if(local.containsKey(name))
            return local.get(name);
        Map<String,Object> m = null;
        //search in options
        m = Options.getStorage().getMap("table/"+name);

        //search in classpath
        if(Objects.isNullOrEmpty(m)){
            try{
                String s = IOUtils.toString(TablesFactory.class.getResourceAsStream("table/" + name + ".cfg"), "UTF-8");
                if(!Objects.isNullOrEmpty(s)){
                    m = Options.getStorage().parseToMap(s);
                }
                LOG.info("Table descriptor "+name+" is read from classpath");
            }catch (Throwable e){}
        }else{
            LOG.info("Table descriptor "+name+" is read from config");
        }

        if(Objects.isNullOrEmpty(m)){
            throw new S1SystemError("Table config file not found: "+name);
        }

        //parse config
        String cls = Objects.get(m,"class");
        Table s = null;
        try{
            s = (Table)Class.forName(cls).newInstance();
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }

        //init
        s.setName(name);
        s.fromMap(m);
        s.init();

        local.put(name,s);
        return s;
    }

}
