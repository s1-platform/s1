package org.s1.test;

import org.s1.misc.Closure;

import java.util.List;

/**
 * Load test thread
 */
public class LoadTestThread implements Runnable {
    private final int index;
    private final List<LoadTestUtils.ResultBean> results;
    private final Closure closure;

    /**
     * @param i
     * @param results
     * @param closure
     */
    public LoadTestThread(int i, List<LoadTestUtils.ResultBean> results, Closure closure) {
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
            e.printStackTrace();
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