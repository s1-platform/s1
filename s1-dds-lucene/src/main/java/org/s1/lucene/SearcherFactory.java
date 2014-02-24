package org.s1.lucene;

import org.s1.S1SystemError;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.misc.IOUtils;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.s1.script.S1ScriptEngine;
import org.s1.script.ScriptException;
import org.s1.table.format.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Searcher factory
 */
public class SearcherFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SearcherFactory.class);

    private static final Map<String,FullTextSearcher> local = Objects.newHashMap();

    public static final String SCRIPT_ENGINE_PATH = "lucene.scriptEngine";

    /**
     *
     * @param name
     * @return
     */
    public static synchronized FullTextSearcher getSearcher(String name){
        if(local.containsKey(name))
            return local.get(name);
        Map<String,Object> m = null;
        //search in options
        m = Options.getStorage().getMap("search/"+name);

        //search in classpath
        if(Objects.isNullOrEmpty(m)){
            try{
                String s = IOUtils.toString(SearcherFactory.class.getResourceAsStream("search/"+name+".cfg"),"UTF-8");
                if(!Objects.isNullOrEmpty(s)){
                    m = Options.getStorage().parseToMap(s);
                }
                LOG.info("Search descriptor "+name+" is read from classpath");
            }catch (Throwable e){}
        }else{
            LOG.info("Search descriptor "+name+" is read from config");
        }

        if(Objects.isNullOrEmpty(m)){
            throw new IllegalArgumentException("Search config file not found: "+name);
        }

        //parse config
        String cls = Objects.get(m,"class",FullTextSearcher.class.getName());
        FullTextSearcher s = null;
        try{
            s = (FullTextSearcher)Class.forName(cls).newInstance();
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }

        Map<String,FieldBean> fields = Objects.newHashMap();
        List<Map<String,Object>> l = Objects.get(m,"fields");
        if(l!=null){
            for(Map<String,Object> f:l){
                String n = Objects.get(f,"name");
                FieldBean fb = new FieldBean(String.class,1.0F);
                fb.fromMap(f);
                fields.put(n,fb);
            }
        }

        s.setName(name);
        s.setReaderLivetime(Objects.get(Long.class, m, "readerLivetime", 600000L));
        s.setWriterLivetime(Objects.get(Long.class, m, "writerLivetime", 1800000L));
        s.setFields(fields);

        final S1ScriptEngine scriptEngine = new S1ScriptEngine(SCRIPT_ENGINE_PATH);
        final String filterStr = Objects.get(m,"prepareFilter","").trim();
        if(!Objects.isNullOrEmpty(filterStr)){
            s.setPrepareFilter(new Closure<Query, Object>() {
                @Override
                public Object call(Query input) throws ClosureException {
                    try {
                        Map<String, Object> m = input.toMap();
                        scriptEngine.eval(filterStr, Objects.newHashMap(String.class, Object.class,
                                "filter", m));
                        input.fromMap(m);
                    } catch (ScriptException e) {
                        ClosureException.wrap(e);
                    }
                    return null;
                }
            });
        }

        s.init();

        local.put(name,s);

        return s;
    }

}
