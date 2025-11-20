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
 * Enables native Hystrix support in Spring applications.
 * Add this annotation to a Spring @Configuration class to enable Hystrix circuit breakers
 * via the @HystrixCommand annotation without requiring Spring Cloud.
 *
 * <p>Usage example:</p>
 * <pre>
 * {@literal @}Configuration
 * {@literal @}EnableHystrix
 * public class ApplicationConfig {
 *     // Spring beans configuration
 * }
 * </pre>
 *
 * <p>This will automatically register the HystrixCommandAspect and enable AspectJ auto-proxying
 * to intercept methods annotated with @HystrixCommand.</p>
 *
 * @since 1.6.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableHystrix {

    /**
     * Whether to enable Hystrix metrics stream endpoint.
     * Default is true.
     *
     * @return true if metrics stream should be enabled
     */
    boolean enableMetricsStream() default true;

    /**
     * Whether to enable Hystrix request context initialization.
     * When enabled, HystrixRequestContext will be initialized for each web request.
     * Default is true.
     *
     * @return true if request context should be automatically initialized
     */
    boolean enableRequestContext() default true;
}
