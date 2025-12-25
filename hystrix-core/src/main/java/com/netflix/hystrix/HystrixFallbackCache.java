/**
 * Copyright 2012 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.hystrix;

import rx.Observable;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cache for fallback results with TTL (Time-To-Live) expiration.
 * This cache stores successful fallback execution results and reuses them
 * within a configurable time window to avoid repeated fallback execution.
 *
 * @param <R> the return type
 */
/* package */ class HystrixFallbackCache<R> {

    private static class CachedFallbackResult<R> {
        private final Observable<R> cachedObservable;
        private final long expirationTimeInMillis;

        public CachedFallbackResult(Observable<R> observable, long expirationTimeInMillis) {
            this.cachedObservable = observable;
            this.expirationTimeInMillis = expirationTimeInMillis;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTimeInMillis;
        }

        public Observable<R> getCachedObservable() {
            return cachedObservable;
        }
    }

    private final AtomicReference<CachedFallbackResult<R>> cachedResult = new AtomicReference<CachedFallbackResult<R>>(null);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    /**
     * Attempts to retrieve a cached fallback result if one exists and hasn't expired.
     *
     * @return Observable containing cached fallback result, or null if cache miss or expired
     */
    public Observable<R> getCachedFallback() {
        CachedFallbackResult<R> cached = cachedResult.get();
        if (cached != null && !cached.isExpired()) {
            cacheHits.incrementAndGet();
            return cached.getCachedObservable();
        }
        cacheMisses.incrementAndGet();
        return null;
    }

    /**
     * Caches a successful fallback result with the specified TTL.
     *
     * @param fallbackObservable the Observable containing the fallback result
     * @param ttlInMillis time-to-live in milliseconds for this cached result
     */
    public void cacheFallbackResult(Observable<R> fallbackObservable, long ttlInMillis) {
        long expirationTime = System.currentTimeMillis() + ttlInMillis;
        // Cache the observable with replay(1) so multiple subscribers get the same result
        Observable<R> replayObservable = fallbackObservable.cache();
        cachedResult.set(new CachedFallbackResult<R>(replayObservable, expirationTime));
    }

    /**
     * Clears the cached fallback result.
     */
    public void clear() {
        cachedResult.set(null);
    }

    /**
     * Gets the number of cache hits.
     *
     * @return cache hit count
     */
    public long getCacheHits() {
        return cacheHits.get();
    }

    /**
     * Gets the number of cache misses.
     *
     * @return cache miss count
     */
    public long getCacheMisses() {
        return cacheMisses.get();
    }
}
