/**
 * Copyright 2013 Netflix, Inc.
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
 * Priority levels for Hystrix command execution.
 * <p>
 * Commands with higher priority will be given preference during execution
 * when resources are constrained. This affects thread pool queue ordering,
 * semaphore acquisition, and fallback execution.
 * <p>
 * Priority levels (from highest to lowest):
 * <ul>
 * <li>CRITICAL - Reserved for business-critical operations that must execute</li>
 * <li>HIGH - Important operations that should execute before normal traffic</li>
 * <li>NORMAL - Default priority for regular operations (default)</li>
 * <li>LOW - Background or batch operations that can be delayed</li>
 * <li>BACKGROUND - Lowest priority for non-critical background tasks</li>
 * </ul>
 * 
 * @since 1.6
 */
public enum HystrixCommandPriority {
    /**
     * Critical priority - reserved for business-critical operations.
     * Executes before all other priority levels.
     */
    CRITICAL(4),
    
    /**
     * High priority - important operations that should execute before normal traffic.
     */
    HIGH(3),
    
    /**
     * Normal priority - default for regular operations.
     */
    NORMAL(2),
    
    /**
     * Low priority - background or batch operations.
     */
    LOW(1),
    
    /**
     * Background priority - lowest priority for non-critical tasks.
     */
    BACKGROUND(0);
    
    private final int value;
    
    HystrixCommandPriority(int value) {
        this.value = value;
    }
    
    /**
     * Returns the numeric value of this priority level.
     * Higher numbers indicate higher priority.
     * 
     * @return numeric priority value
     */
    public int getValue() {
        return value;
    }
    
    /**
     * Compares this priority to another priority.
     * 
     * @param other the priority to compare to
     * @return positive if this priority is higher, negative if lower, 0 if equal
     */
    public int compareTo(HystrixCommandPriority other) {
        return Integer.compare(this.value, other.value);
    }
}
