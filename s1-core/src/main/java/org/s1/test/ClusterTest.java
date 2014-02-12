package org.s1.test;

import org.s1.cluster.node.ClusterNode;

/**
 * Base class for cluster tests
 */
public abstract class ClusterTest extends BasicTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ClusterNode.start();
    }

}
