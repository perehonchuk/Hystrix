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
package com.netflix.hystrix;

/**
 * Interface that request arguments can implement to provide priority-based sharding
 * in HystrixCollapser batching.
 * <p>
 * When request arguments implement this interface, the collapser will automatically
 * group requests by priority level, creating separate shards (and thus separate
 * HystrixCommand executions) for each priority level.
 * <p>
 * This allows high-priority requests to be processed independently from low-priority
 * requests, preventing low-priority batch processing from delaying critical requests.
 */
public interface HystrixRequestPriority {
    
    /**
     * Returns the priority level for this request.
     * <p>
     * Requests with the same priority will be batched together in the same shard.
     * Different priority levels will be executed as separate batches.
     * <p>
     * Standard priority levels (by convention, though any int value is supported):
     * <ul>
     * <li>100+ = CRITICAL - highest priority, processed first</li>
     * <li>50-99 = HIGH - elevated priority</li>
     * <li>0-49 = NORMAL - standard priority (default)</li>
     * <li>-50 to -1 = LOW - reduced priority</li>
     * <li>-100 or less = BACKGROUND - lowest priority</li>
     * </ul>
     * 
     * @return int priority level (higher values = higher priority)
     */
    int getPriority();
}
