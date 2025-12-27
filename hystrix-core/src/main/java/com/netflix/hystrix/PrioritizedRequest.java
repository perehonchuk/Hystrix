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
 * Interface for request arguments that support priority-based batching in {@link HystrixCollapser}.
 * <p>
 * When request arguments implement this interface, the collapser can automatically group
 * requests by priority level, allowing high-priority requests to be batched and executed
 * together separately from lower-priority requests.
 * <p>
 * This is useful when you want to ensure that critical requests are processed together
 * and can potentially be routed to different backend resources or handled with different
 * quality-of-service guarantees.
 */
public interface PrioritizedRequest {

    /**
     * Returns the priority level of this request.
     * <p>
     * Lower values indicate higher priority (e.g., 0 is highest priority).
     * <p>
     * Requests with the same priority value will be batched together.
     *
     * @return the priority level (0 = highest priority, higher numbers = lower priority)
     */
    int getPriority();
}
