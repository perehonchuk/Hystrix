/**
 * Copyright 2015 Netflix, Inc.
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

/**
 * Listener interface for receiving notifications when cache entries are invalidated.
 * <p>
 * This allows external systems to react to cache invalidation events, such as:
 * - Invalidating downstream caches
 * - Logging cache invalidation for debugging
 * - Triggering pre-fetch operations
 * - Updating metrics
 * <p>
 * Listeners are registered per HystrixCommandKey or HystrixCollapserKey and will be notified
 * whenever a cache entry matching their scope is invalidated.
 *
 * @since 1.5.0
 */
public interface HystrixCacheInvalidationListener {

    /**
     * Called when a cache entry is invalidated.
     * <p>
     * This method is called synchronously during cache invalidation, so implementations
     * should be fast and non-blocking. For expensive operations, consider offloading
     * to a separate thread or queue.
     *
     * @param commandKey the command key associated with the cache entry
     * @param cacheKey the specific cache key that was invalidated
     * @param reason the reason for invalidation (e.g., "explicit", "timeout", "pattern")
     */
    void onCacheInvalidated(HystrixCommandKey commandKey, String cacheKey, String reason);

    /**
     * Called when a cache entry is invalidated for a collapser.
     * <p>
     * This method is called synchronously during cache invalidation, so implementations
     * should be fast and non-blocking. For expensive operations, consider offloading
     * to a separate thread or queue.
     *
     * @param collapserKey the collapser key associated with the cache entry
     * @param cacheKey the specific cache key that was invalidated
     * @param reason the reason for invalidation (e.g., "explicit", "timeout", "pattern")
     */
    void onCacheInvalidated(HystrixCollapserKey collapserKey, String cacheKey, String reason);
}
