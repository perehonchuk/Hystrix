/**
 * Copyright 2016 Netflix, Inc.
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
package com.netflix.hystrix;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Request queueing functionality for Hystrix commands.
 * Provides queueing, deduplication, and throttling capabilities.
 */
public class HystrixRequestQueue {

    private static final ConcurrentHashMap<String, HystrixRequestQueue> queues = new ConcurrentHashMap<String, HystrixRequestQueue>();

    private final ConcurrentLinkedQueue<QueuedRequest> requestQueue = new ConcurrentLinkedQueue<QueuedRequest>();
    private final AtomicInteger queueSize = new AtomicInteger(0);
    private final ConcurrentHashMap<String, QueuedRequest> deduplicationMap = new ConcurrentHashMap<String, QueuedRequest>();

    public static class QueuedRequest {
        private final String requestKey;
        private final long enqueuedTime;
        private volatile boolean processed = false;

        public QueuedRequest(String requestKey) {
            this.requestKey = requestKey;
            this.enqueuedTime = System.currentTimeMillis();
        }

        public String getRequestKey() {
            return requestKey;
        }

        public long getEnqueuedTime() {
            return enqueuedTime;
        }

        public boolean isProcessed() {
            return processed;
        }

        public void markProcessed() {
            this.processed = true;
        }
    }

    public static HystrixRequestQueue getInstance(HystrixCommandKey commandKey) {
        String key = commandKey.name();
        HystrixRequestQueue queue = queues.get(key);
        if (queue == null) {
            queue = new HystrixRequestQueue();
            HystrixRequestQueue existing = queues.putIfAbsent(key, queue);
            if (existing != null) {
                queue = existing;
            }
        }
        return queue;
    }

    public boolean enqueue(QueuedRequest request, int maxSize) {
        if (queueSize.get() >= maxSize) {
            return false;
        }
        requestQueue.offer(request);
        queueSize.incrementAndGet();
        return true;
    }

    public QueuedRequest dequeue() {
        QueuedRequest request = requestQueue.poll();
        if (request != null) {
            queueSize.decrementAndGet();
        }
        return request;
    }

    public int size() {
        return queueSize.get();
    }

    public QueuedRequest findOrCreateDeduplicatedRequest(String cacheKey) {
        QueuedRequest existing = deduplicationMap.get(cacheKey);
        if (existing != null && !existing.isProcessed()) {
            return existing;
        }
        QueuedRequest newRequest = new QueuedRequest(cacheKey);
        QueuedRequest previous = deduplicationMap.putIfAbsent(cacheKey, newRequest);
        return previous != null ? previous : newRequest;
    }

    public void removeFromDeduplication(String cacheKey) {
        deduplicationMap.remove(cacheKey);
    }

    public static void reset() {
        queues.clear();
    }
}
