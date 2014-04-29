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

package org.s1.testing;

import java.util.List;

/**
 * Load test thread
 */
public class LoadTestThread implements Runnable {
    private final int index;
    private final List<LoadTestUtils.ResultBean> results;
    private final LoadTestUtils.LoadTestProcedure closure;

    /**
     * @param i
     * @param results
     * @param closure
     */
    public LoadTestThread(int i, List<LoadTestUtils.ResultBean> results, LoadTestUtils.LoadTestProcedure closure) {
        index = i;
        this.results = results;
        this.closure = closure;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        boolean success = true;
        Throwable error = null;
        try {
            //TEST
            closure.call(index);
            //END TEST
        } catch (Throwable e) {
            e.printStackTrace(System.out);
            success = false;
            error = e;
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            synchronized (results) {
                results.add(new LoadTestUtils.ResultBean(error, success, elapsed));
            }
        }
    }

}