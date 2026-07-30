/*
 * Copyright (C) Tony Green, LitePal Framework Open Source Project
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

import org.junit.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AsyncExecutorTest {

    @Test
    public void asyncOperationsShareOneBackgroundWorker() throws Exception {
        final int taskCount = 50;
        final CountDownLatch firstTaskStarted = new CountDownLatch(1);
        final CountDownLatch releaseFirstTask = new CountDownLatch(1);
        final CountDownLatch allTasksFinished = new CountDownLatch(taskCount);
        final AtomicInteger runningTasks = new AtomicInteger();
        final AtomicInteger maximumConcurrency = new AtomicInteger();
        final Set<String> threadNames = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < taskCount; i++) {
            final int taskIndex = i;
            TestExecutor executor = new TestExecutor();
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    int running = runningTasks.incrementAndGet();
                    updateMaximum(maximumConcurrency, running);
                    threadNames.add(Thread.currentThread().getName());
                    try {
                        if (taskIndex == 0) {
                            firstTaskStarted.countDown();
                            assertTrue(releaseFirstTask.await(5, TimeUnit.SECONDS));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        runningTasks.decrementAndGet();
                        allTasksFinished.countDown();
                    }
                }
            });
            executor.start();
        }

        assertTrue(firstTaskStarted.await(5, TimeUnit.SECONDS));
        assertEquals(1, runningTasks.get());
        releaseFirstTask.countDown();
        assertTrue(allTasksFinished.await(10, TimeUnit.SECONDS));
        assertEquals(1, maximumConcurrency.get());
        assertEquals(1, threadNames.size());
        assertTrue(threadNames.iterator().next().startsWith("LitePal-Async-"));
    }

    private static void updateMaximum(AtomicInteger maximum, int value) {
        int current;
        do {
            current = maximum.get();
            if (current >= value) {
                return;
            }
        } while (!maximum.compareAndSet(current, value));
    }

    private static final class TestExecutor extends AsyncExecutor {

        void start() {
            execute();
        }
    }
}
