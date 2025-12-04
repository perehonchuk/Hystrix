/**
 * Copyright 2025 Netflix, Inc.
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

import com.netflix.hystrix.AbstractCommand;

import java.util.concurrent.Callable;

/**
 * Wrapper around Callable that carries priority information for priority-based queue ordering.
 * This class implements Comparable to allow PriorityBlockingQueue to order tasks by priority.
 *
 * Higher priority values are executed first. Within the same priority level, tasks are ordered
 * by submission time (FIFO).
 */
public class HystrixPriorityCallable<V> implements Callable<V>, Comparable<HystrixPriorityCallable<V>> {

    private final Callable<V> delegate;
    private final int priority;
    private final long submissionTime;

    public HystrixPriorityCallable(Callable<V> delegate, AbstractCommand.CommandPriority commandPriority) {
        this.delegate = delegate;
        this.priority = commandPriority.getValue();
        this.submissionTime = System.nanoTime();
    }

    public HystrixPriorityCallable(Callable<V> delegate, int priority) {
        this.delegate = delegate;
        this.priority = priority;
        this.submissionTime = System.nanoTime();
    }

    @Override
    public V call() throws Exception {
        return delegate.call();
    }

    @Override
    public int compareTo(HystrixPriorityCallable<V> other) {
        // Higher priority values come first (reverse order)
        int priorityComparison = Integer.compare(other.priority, this.priority);
        if (priorityComparison != 0) {
            return priorityComparison;
        }
        // For same priority, earlier submission time comes first (FIFO)
        return Long.compare(this.submissionTime, other.submissionTime);
    }

    public int getPriority() {
        return priority;
    }

    public long getSubmissionTime() {
        return submissionTime;
    }

    public Callable<V> getDelegate() {
        return delegate;
    }
}
