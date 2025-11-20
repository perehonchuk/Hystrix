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

/**
 * Native Spring Framework integration for Hystrix.
 *
 * <p>This package provides built-in Spring support for Hystrix without requiring
 * external libraries like Spring Cloud. It enables declarative circuit breaker
 * configuration using annotations.</p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>@HystrixCommand annotation for declarative circuit breaker wrapping</li>
 *   <li>@EnableHystrix annotation for simple Spring Boot integration</li>
 *   <li>Automatic fallback method resolution</li>
 *   <li>Spring context propagation across Hystrix threads</li>
 *   <li>Integration with Spring AOP for method interception</li>
 *   <li>Request context initialization for web applications</li>
 * </ul>
 *
 * <h2>Getting Started:</h2>
 * <pre>
 * // 1. Enable Hystrix in your Spring configuration
 * {@literal @}Configuration
 * {@literal @}EnableHystrix
 * public class ApplicationConfig {
 * }
 *
 * // 2. Use @HystrixCommand on your service methods
 * {@literal @}Service
 * public class UserService {
 *
 *     {@literal @}HystrixCommand(
 *         commandKey = "getUser",
 *         groupKey = "UserService",
 *         fallbackMethod = "getUserFallback",
 *         timeoutInMilliseconds = 2000
 *     )
 *     public User getUser(String userId) {
 *         return externalService.fetchUser(userId);
 *     }
 *
 *     public User getUserFallback(String userId) {
 *         return new User(userId, "Default User");
 *     }
 * }
 * </pre>
 *
 * <h2>Advantages over Spring Cloud Netflix:</h2>
 * <ul>
 *   <li>No external dependencies required</li>
 *   <li>Lighter weight integration</li>
 *   <li>Built directly into Hystrix core</li>
 *   <li>Simpler configuration</li>
 *   <li>Better performance due to tight integration</li>
 * </ul>
 *
 * @since 1.6.0
 */
package com.netflix.hystrix.contrib.spring;
