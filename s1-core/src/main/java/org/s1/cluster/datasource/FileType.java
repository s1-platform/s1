package org.s1.cluster.datasource;

import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;
import org.s1.objects.schema.ComplexType;

import java.util.Map;

/**
 * File complex type
 */
public class FileType extends ComplexType {

    protected String getGroup(){
        return Objects.get(getConfig(),"group");
    }

    @Override
    public Map<String, Object> expand(final Map<String, Object> m, boolean expand) throws Exception {
        String id = Objects.get(m,"group");
        FileStorage.read(getGroup(),id,new Closure<FileStorage.FileReadBean, Object>() {
            @Override
            public Object call(FileStorage.FileReadBean input) throws ClosureException {
                m.putAll(input.getMeta().toMap());
                return null;
            }
        });
        return m;
    }

    @Override
    public Map<String, Object> validate(Map<String, Object> m) throws Exception {
        String id = Objects.get(m,"group");
        FileStorage.read(getGroup(),id,null);
        return m;
    }
}
