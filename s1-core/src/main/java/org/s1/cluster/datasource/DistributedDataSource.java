package org.s1.cluster.datasource;

import java.util.Map;

/**
 * s1
 * User: GPykhov
 * Date: 17.12.13
 * Time: 14:32
 */
public abstract class DistributedDataSource {

    public abstract void runWriteCommand(String command, Map<String,Object> params);

}
