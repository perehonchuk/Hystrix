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
 * Wrapper for Runnable that makes it Comparable based on priority level.
 * Used by PriorityBlockingQueue to order tasks based on their execution priority.
 * <p>
 * Higher priority values indicate higher priority (will be executed first).
 */
public class HystrixPriorityRunnable implements Runnable, Comparable<HystrixPriorityRunnable> {

    private final Runnable delegate;
    private final int priority;
    private final long sequenceNumber;
    private static final java.util.concurrent.atomic.AtomicLong sequenceGenerator = new java.util.concurrent.atomic.AtomicLong(0);

    public HystrixPriorityRunnable(Runnable delegate, int priority) {
        this.delegate = delegate;
        this.priority = priority;
        this.sequenceNumber = sequenceGenerator.getAndIncrement();
    }

    @Override
    public void run() {
        delegate.run();
    }

    @Override
    public int compareTo(HystrixPriorityRunnable other) {
        // Higher priority value = higher priority = executed first (smaller in priority queue)
        int priorityComparison = Integer.compare(other.priority, this.priority);
        if (priorityComparison != 0) {
            return priorityComparison;
        }
        // If priorities are equal, use FIFO order via sequence number
        return Long.compare(this.sequenceNumber, other.sequenceNumber);
    }

    public int getPriority() {
        return priority;
    }

    public Runnable getDelegate() {
        return delegate;
    }
}
