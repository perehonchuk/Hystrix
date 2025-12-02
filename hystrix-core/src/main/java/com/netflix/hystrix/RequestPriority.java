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
 * Priority levels for request collapsing.
 * Requests with the same priority level will be batched together.
 * When priority-based batching is enabled, requests of different priorities
 * will not be collapsed into the same batch.
 */
public enum RequestPriority {
    /**
     * Highest priority requests - executed first when multiple batches are ready
     */
    HIGH(3),

    /**
     * Normal priority requests - default priority level
     */
    NORMAL(2),

    /**
     * Low priority requests - executed after higher priority batches
     */
    LOW(1);

    private final int level;

    RequestPriority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
