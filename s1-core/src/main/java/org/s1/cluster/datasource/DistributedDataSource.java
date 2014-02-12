package org.s1.cluster.datasource;

import java.util.Map;

/**
 * Base class for distributed data sources
 */
public abstract class DistributedDataSource {

    public abstract void runWriteCommand(String command, Map<String,Object> params);

}
