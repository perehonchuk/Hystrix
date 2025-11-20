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
package com.netflix.hystrix.contrib.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Native Spring integration annotation for marking methods to be wrapped with Hystrix circuit breaker.
 * This annotation provides declarative circuit breaker functionality without requiring Spring Cloud.
 *
 * <p>Usage example:</p>
 * <pre>
 * {@literal @}HystrixCommand(commandKey = "getUserById", groupKey = "UserService",
 *                fallbackMethod = "getUserByIdFallback")
 * public User getUserById(String id) {
 *     return userRepository.findById(id);
 * }
 *
 * public User getUserByIdFallback(String id) {
 *     return new User(id, "Default User");
 * }
 * </pre>
 *
 * @since 1.6.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HystrixCommand {

    /**
     * The command key to identify this Hystrix command.
     * If not specified, the method name will be used.
     *
     * @return command key name
     */
    String commandKey() default "";

    /**
     * The command group key for this Hystrix command.
     * Commands in the same group share the same thread pool and circuit breaker statistics.
     * If not specified, the declaring class name will be used.
     *
     * @return group key name
     */
    String groupKey() default "";

    /**
     * The thread pool key for this Hystrix command.
     * If not specified, the group key will be used.
     *
     * @return thread pool key name
     */
    String threadPoolKey() default "";

    /**
     * Name of the fallback method in the same class.
     * The fallback method must have the same signature as the annotated method.
     *
     * @return fallback method name
     */
    String fallbackMethod() default "";

    /**
     * Timeout in milliseconds for the command execution.
     * Default is 1000ms.
     *
     * @return timeout in milliseconds
     */
    int timeoutInMilliseconds() default 1000;

    /**
     * Circuit breaker error threshold percentage (0-100).
     * Circuit will open when error percentage exceeds this value.
     * Default is 50%.
     *
     * @return error threshold percentage
     */
    int errorThresholdPercentage() default 50;

    /**
     * Minimum number of requests in rolling window before circuit breaker can trip.
     * Default is 20 requests.
     *
     * @return minimum request volume threshold
     */
    int requestVolumeThreshold() default 20;

    /**
     * Time in milliseconds circuit breaker stays open before attempting to close.
     * Default is 5000ms.
     *
     * @return sleep window in milliseconds
     */
    int sleepWindowInMilliseconds() default 5000;

    /**
     * Whether to enable request caching for this command.
     * Default is true.
     *
     * @return true if request caching enabled
     */
    boolean requestCacheEnabled() default true;

    /**
     * Isolation strategy: THREAD or SEMAPHORE.
     * Default is THREAD.
     *
     * @return isolation strategy
     */
    IsolationStrategy isolationStrategy() default IsolationStrategy.THREAD;

    /**
     * Core thread pool size when using THREAD isolation.
     * Default is 10.
     *
     * @return core thread pool size
     */
    int corePoolSize() default 10;

    /**
     * Maximum semaphore permits when using SEMAPHORE isolation.
     * Default is 10.
     *
     * @return maximum concurrent requests
     */
    int maxConcurrentRequests() default 10;
}
