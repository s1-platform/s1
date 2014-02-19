package org.s1.lucene;

import org.s1.cluster.datasource.DistributedDataSource;
import org.s1.objects.Objects;

import java.util.Map;

/**
 * Lucene distributed data source implementation
 */
public class LuceneDDS extends DistributedDataSource{

    @Override
    public void runWriteCommand(String command, Map<String, Object> params) {
        String name = Objects.get(params,"name");
        if(Objects.isNullOrEmpty(name)){
            return;
        }
        FullTextSearcher s = SearcherFactory.getSearcher(name);
        if("add".equals(command)){
            String id = Objects.get(params,"id");
            Map<String,Object> data = Objects.get(params,"data");
            if(Objects.isNullOrEmpty(id) || Objects.isNullOrEmpty(data))
                return;
            s.localAddDocument(id,data);
        }else if("set".equals(command)){
            String id = Objects.get(params,"id");
            Map<String,Object> data = Objects.get(params,"data");
            if(Objects.isNullOrEmpty(id) || Objects.isNullOrEmpty(data))
                return;
            s.localSetDocument(id,data);
        }else if("remove".equals(command)){
            String id = Objects.get(params,"id");
            if(Objects.isNullOrEmpty(id))
                return;
            s.localRemoveDocument(id);
        }
    }
}
