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
package com.netflix.hystrix.examples.basic;

import com.netflix.hystrix.HystrixCacheInvalidationListener;
import com.netflix.hystrix.HystrixCollapserKey;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;

/**
 * Example demonstrating cache invalidation listeners.
 * <p>
 * This command shows how to register a listener to be notified when
 * cache entries are invalidated, allowing coordination with external
 * caching systems or triggering pre-fetch operations.
 */
public class CommandWithCacheInvalidationListener extends HystrixCommand<String> {

    private final String key;

    public CommandWithCacheInvalidationListener(String key) {
        super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"));
        this.key = key;

        // Register a listener to be notified of cache invalidations
        registerCacheInvalidationListener(new HystrixCacheInvalidationListener() {
            @Override
            public void onCacheInvalidated(HystrixCommandKey commandKey, String cacheKey, String reason) {
                System.out.println("Cache invalidated for command " + commandKey.name() +
                                   ", key: " + cacheKey + ", reason: " + reason);
                // Here you could:
                // - Invalidate downstream caches
                // - Trigger pre-fetch operations
                // - Update metrics or logging
                // - Notify other services via message queue
            }

            @Override
            public void onCacheInvalidated(HystrixCollapserKey collapserKey, String cacheKey, String reason) {
                // Not applicable for commands
            }
        });
    }

    @Override
    protected String run() {
        return "Value for: " + key;
    }

    @Override
    protected String getCacheKey() {
        return key;
    }
}
