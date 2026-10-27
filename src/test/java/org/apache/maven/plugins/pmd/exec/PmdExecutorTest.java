/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.pmd.exec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import net.sourceforge.pmd.benchmark.TimeTracker;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PmdExecutorTest {

    @Test
    void skipsBenchmarkReportWhenTimingReportIsMissing() throws Exception {
        Path output = Files.createTempFile("pmd-benchmark", ".txt");
        Files.delete(output);

        new PmdExecutor(new PmdRequest()).writeBenchmarkReport(null, output.toString(), "UTF-8");

        assertFalse(Files.exists(output));
    }

    @RepeatedTest(5)
    void benchmarkExecutionsDoNotOverlap() throws Exception {
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondAttempting = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);
        AtomicInteger activeExecutions = new AtomicInteger();
        AtomicInteger maximumActiveExecutions = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread first = new Thread(() -> {
            try {
                PmdExecutor.withTimeTrackerLock(true, () -> {
                    TimeTracker.startGlobalTracking();
                    recordExecution(activeExecutions, maximumActiveExecutions);
                    firstEntered.countDown();
                    try {
                        await(releaseFirst);
                    } finally {
                        TimeTracker.stopGlobalTracking();
                        activeExecutions.decrementAndGet();
                    }
                    return null;
                });
            } catch (Throwable throwable) {
                failure.compareAndSet(null, throwable);
            }
        });
        Thread second = new Thread(() -> {
            secondAttempting.countDown();
            try {
                PmdExecutor.withTimeTrackerLock(true, () -> {
                    TimeTracker.startGlobalTracking();
                    secondEntered.countDown();
                    recordExecution(activeExecutions, maximumActiveExecutions);
                    try {
                        return null;
                    } finally {
                        TimeTracker.stopGlobalTracking();
                        activeExecutions.decrementAndGet();
                    }
                });
            } catch (Throwable throwable) {
                failure.compareAndSet(null, throwable);
            }
        });

        first.start();
        try {
            assertTrue(firstEntered.await(5, TimeUnit.SECONDS), "First benchmark execution did not start");
            second.start();
            assertTrue(secondAttempting.await(5, TimeUnit.SECONDS), "Second benchmark execution did not start");

            boolean secondBlockedOnBenchmarkLock = awaitBlockedOrEntered(second, secondEntered);

            releaseFirst.countDown();
            first.join(TimeUnit.SECONDS.toMillis(5));
            second.join(TimeUnit.SECONDS.toMillis(5));

            assertFalse(first.isAlive(), "First benchmark execution did not finish");
            assertFalse(second.isAlive(), "Second benchmark execution did not finish");
            assertTrue(secondBlockedOnBenchmarkLock, "Second execution entered while the first was active");
            assertEquals(1, maximumActiveExecutions.get(), "Benchmark executions overlapped");
            assertNull(failure.get());
        } finally {
            releaseFirst.countDown();
            first.join(TimeUnit.SECONDS.toMillis(5));
            if (second.getState() != Thread.State.NEW) {
                second.join(TimeUnit.SECONDS.toMillis(5));
            }
        }
    }

    @RepeatedTest(5)
    void benchmarkExecutionExcludesNonBenchmarkExecutions() throws Exception {
        CountDownLatch benchmarkEntered = new CountDownLatch(1);
        CountDownLatch releaseBenchmark = new CountDownLatch(1);
        CountDownLatch normalAttempting = new CountDownLatch(1);
        CountDownLatch normalEntered = new CountDownLatch(1);
        CountDownLatch benchmarkStopped = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread benchmark = new Thread(() -> {
            try {
                PmdExecutor.withTimeTrackerLock(true, () -> {
                    TimeTracker.startGlobalTracking();
                    benchmarkEntered.countDown();
                    try {
                        await(releaseBenchmark);
                    } finally {
                        TimeTracker.stopGlobalTracking();
                        benchmarkStopped.countDown();
                    }
                    return null;
                });
            } catch (Throwable throwable) {
                failure.compareAndSet(null, throwable);
            }
        });
        Thread normal = new Thread(() -> {
            normalAttempting.countDown();
            try {
                PmdExecutor.withTimeTrackerLock(false, () -> {
                    assertEquals(
                            0, benchmarkStopped.getCount(), "A non-benchmark run overlapped global benchmark tracking");
                    normalEntered.countDown();
                    return null;
                });
            } catch (Throwable throwable) {
                failure.compareAndSet(null, throwable);
            }
        });

        benchmark.start();
        try {
            assertTrue(benchmarkEntered.await(5, TimeUnit.SECONDS), "Benchmark execution did not start");
            normal.start();
            assertTrue(normalAttempting.await(5, TimeUnit.SECONDS), "Normal execution did not start");

            boolean normalBlockedUntilTrackingStopped = awaitBlockedOrEntered(normal, normalEntered);

            releaseBenchmark.countDown();
            benchmark.join(TimeUnit.SECONDS.toMillis(5));
            normal.join(TimeUnit.SECONDS.toMillis(5));

            assertFalse(benchmark.isAlive(), "Benchmark execution did not finish");
            assertFalse(normal.isAlive(), "Normal execution did not finish");
            assertTrue(
                    normalBlockedUntilTrackingStopped,
                    "Normal execution entered while global benchmark tracking was active");
            assertNull(failure.get());
        } finally {
            releaseBenchmark.countDown();
            benchmark.join(TimeUnit.SECONDS.toMillis(5));
            if (normal.getState() != Thread.State.NEW) {
                normal.join(TimeUnit.SECONDS.toMillis(5));
            }
        }
    }

    @Test
    void nonBenchmarkExecutionsCanOverlap() throws Exception {
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);
        AtomicInteger activeExecutions = new AtomicInteger();
        AtomicInteger maximumActiveExecutions = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread first = new Thread(() ->
                runNormalExecution(firstEntered, releaseFirst, activeExecutions, maximumActiveExecutions, failure));
        Thread second = new Thread(() -> {
            try {
                PmdExecutor.withTimeTrackerLock(false, () -> {
                    recordExecution(activeExecutions, maximumActiveExecutions);
                    secondEntered.countDown();
                    activeExecutions.decrementAndGet();
                    return null;
                });
            } catch (Throwable throwable) {
                failure.compareAndSet(null, throwable);
            }
        });

        first.start();
        try {
            assertTrue(firstEntered.await(5, TimeUnit.SECONDS), "First normal execution did not start");
            second.start();
            assertTrue(secondEntered.await(5, TimeUnit.SECONDS), "Normal executions were unnecessarily serialized");
            releaseFirst.countDown();
            first.join(TimeUnit.SECONDS.toMillis(5));
            second.join(TimeUnit.SECONDS.toMillis(5));

            assertFalse(first.isAlive(), "First normal execution did not finish");
            assertFalse(second.isAlive(), "Second normal execution did not finish");
            assertEquals(2, maximumActiveExecutions.get());
            assertNull(failure.get());
        } finally {
            releaseFirst.countDown();
            first.join(TimeUnit.SECONDS.toMillis(5));
            if (second.getState() != Thread.State.NEW) {
                second.join(TimeUnit.SECONDS.toMillis(5));
            }
        }
    }

    private static void runNormalExecution(
            CountDownLatch entered,
            CountDownLatch release,
            AtomicInteger active,
            AtomicInteger maximum,
            AtomicReference<Throwable> failure) {
        try {
            PmdExecutor.withTimeTrackerLock(false, () -> {
                recordExecution(active, maximum);
                entered.countDown();
                try {
                    await(release);
                } finally {
                    active.decrementAndGet();
                }
                return null;
            });
        } catch (Throwable throwable) {
            failure.compareAndSet(null, throwable);
        }
    }

    private static void recordExecution(AtomicInteger active, AtomicInteger maximum) {
        int concurrent = active.incrementAndGet();
        maximum.accumulateAndGet(concurrent, Math::max);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting to release benchmark execution");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static boolean awaitBlockedOrEntered(Thread thread, CountDownLatch entered) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            Thread.State state = thread.getState();
            if (state == Thread.State.BLOCKED || state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) {
                return true;
            }
            if (entered.getCount() == 0 || state == Thread.State.TERMINATED) {
                return false;
            }
            Thread.yield();
        }
        return false;
    }
}
