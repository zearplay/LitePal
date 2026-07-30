/*
 * Copyright (C)  Tony Green, LitePal Framework Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.litepal.crud.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple async executor to run tasks in background thread.
 *
 * @author Tony Green
 * @since 2017/2/22
 */
public abstract class AsyncExecutor {

    /**
     * LitePal used to create one thread for every asynchronous call. CRUD operations were
     * serialized by a global monitor anyway, so bursts of calls only created many blocked
     * threads. A single worker keeps the original serialized behavior without exhausting
     * threads and making the application unresponsive.
     */
    private static final ExecutorService EXECUTOR = createExecutor();

    /**
     * Task that pending to run.
     */
    private Runnable pendingTask;

    private static ExecutorService createExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                1,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new LitePalThreadFactory());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    /**
     * Submit a task for pending executing.
     * @param task
     *          The task with specific database operation.
     */
    public void submit(Runnable task) {
        pendingTask = task;
    }

    /**
     * Run the pending task in background thread.
     */
    void execute() {
        if (pendingTask != null) {
            EXECUTOR.execute(pendingTask);
        }
    }

    private static final class LitePalThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable,
                    "LitePal-Async-" + threadNumber.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }

}
