/**
 * Copyright 2012 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.hystrix.strategy.concurrency;

/**
 * Wrapper for Runnable tasks that adds priority-based ordering for thread pool execution.
 * Tasks with higher priority values will be executed before tasks with lower priorities.
 *
 * This class implements Comparable to enable use with PriorityBlockingQueue.
 */
public class PriorityRunnable implements Runnable, Comparable<PriorityRunnable> {
    private final Runnable delegate;
    private final int priority;
    private final long sequenceNumber; // For FIFO ordering within same priority

    private static final java.util.concurrent.atomic.AtomicLong sequenceGenerator =
        new java.util.concurrent.atomic.AtomicLong(0);

    /**
     * Creates a PriorityRunnable with the specified priority.
     *
     * @param delegate The actual runnable task to execute
     * @param priority Priority level (0-10, higher values execute first)
     */
    public PriorityRunnable(Runnable delegate, int priority) {
        this.delegate = delegate;
        this.priority = priority;
        this.sequenceNumber = sequenceGenerator.getAndIncrement();
    }

    @Override
    public void run() {
        delegate.run();
    }

    public int getPriority() {
        return priority;
    }

    public Runnable getDelegate() {
        return delegate;
    }

    @Override
    public int compareTo(PriorityRunnable other) {
        // Higher priority values should come first (so we negate the comparison)
        int priorityComparison = Integer.compare(other.priority, this.priority);

        if (priorityComparison != 0) {
            return priorityComparison;
        }

        // For same priority, maintain FIFO order using sequence number
        return Long.compare(this.sequenceNumber, other.sequenceNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PriorityRunnable)) return false;

        PriorityRunnable other = (PriorityRunnable) obj;
        return this.priority == other.priority &&
               this.sequenceNumber == other.sequenceNumber;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + priority;
        result = 31 * result + (int)(sequenceNumber ^ (sequenceNumber >>> 32));
        return result;
    }
}
