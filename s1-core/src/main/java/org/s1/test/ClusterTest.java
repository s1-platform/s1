package org.s1.test;

import org.s1.cluster.node.ClusterNode;

/**
 * s1v2
 * User: GPykhov
 * Date: 23.01.14
 * Time: 12:26
 */
public abstract class ClusterTest extends BasicTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ClusterNode.start();
    }

}
