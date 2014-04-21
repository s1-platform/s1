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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Load testing helper
 */
public class LoadTestUtils {

    public static interface LoadTestProcedure{
        public void call(int i) throws Exception;
    }

    /**
     * Run load test
     *
     * @param message
     * @param total
     * @param parallel
     * @param test
     * @return
     */
    public static int run(String message, int total, int parallel, LoadTestProcedure test) {
        long start = System.currentTimeMillis();
        List<ResultBean> results = new ArrayList<ResultBean>();
        ExecutorService executor = Executors.newFixedThreadPool(parallel,new ThreadFactory() {
            private AtomicInteger i = new AtomicInteger(-1);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "LoadTestThread-"+i.incrementAndGet());
            }
        });
        for (int i = 0; i < total; i++) {
            Runnable worker = new LoadTestThread(i, results, test);
            executor.execute(worker);
        }
        executor.shutdown();
        long heartbeat = System.currentTimeMillis();
        while (!executor.isTerminated()) {
            if (System.currentTimeMillis() - heartbeat > 5000) {
                heartbeat = System.currentTimeMillis();
                int processed = results.size();
                long remains = -1;
                if (processed > 0)
                    remains = (long)((total - processed) * 1.0D * ((heartbeat - start)*1.0D / processed*1.0D));
                System.out.println("[... " + processed + " of " + total + " tests done in " + (heartbeat - start) + "ms; " +
                        "remaining " + (total - processed) + " tests, " + remains + "ms left ...]");
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                break;
            }
        }

        String msg = "finish " + message + "\n";
        msg += "total count: \t\t" + results.size() + "\n";
        msg += "total time: \t\t" + (System.currentTimeMillis() - start) + "ms\n";
        long min = Integer.MAX_VALUE;
        long max = 0;
        long avg = 0;
        int success = 0;
        int error = 0;
        Map<String,Integer> errors = new HashMap<String, Integer>();
        for (ResultBean r : results) {
            if (r.getElapsed() > max)
                max = r.getElapsed();
            if (r.getElapsed() < min)
                min = r.getElapsed();
            avg += r.getElapsed();
            if (r.isSuccess())
                success++;
            else {
                error++;
                String err = r.getError().getMessage();
                if(errors.containsKey(err))
                    errors.put(err,errors.get(err)+1);
                else
                    errors.put(err,1);
            }
        }
        for (Map.Entry<String,Integer> e : errors.entrySet()) {
            msg += ("ERROR (" + e.getValue() + "): " + e.getKey() + "\n");
        }

        avg = avg / results.size();
        msg += ("success count: \t\t" + success + "\n");
        msg += ("error count: \t\t" + error + "\n");
        msg += ("total time min: \t\t" + min + "ms" + "\n");
        msg += ("total time max: \t\t" + max + "ms" + "\n");
        msg += ("total time avg: \t\t" + avg + "ms" + "\n");
        System.err.println(msg);

        System.err.println("ACTIVE THREADS_______________________"+Thread.activeCount());

        return success;
    }

    /**
     * Result of load test run
     */
    public static class ResultBean {
        private Throwable error;
        private boolean success;
        private long elapsed;

        public ResultBean(Throwable error, boolean success, long elapsed) {
            this.error = error;
            this.success = success;
            this.elapsed = elapsed;
        }

        public Throwable getError() {
            return error;
        }

        public void setError(Throwable error) {
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public long getElapsed() {
            return elapsed;
        }

        public void setElapsed(long elapsed) {
            this.elapsed = elapsed;
        }
    }

}
