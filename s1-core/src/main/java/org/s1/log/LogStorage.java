package org.s1.log;

import org.s1.table.format.Query;

import java.util.List;
import java.util.Map;

/**
 * Log storage that can be listed
 */
public class LogStorage {

    /**
     * Get log messages
     *
     * @param list
     * @param search
     * @param skip
     * @param max
     * @return
     */
    public long list(List<Map<String,Object>> list, Query search, int skip, int max){
        return 0;
    }

}
