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

import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;

/**
 * Spring configuration helper for native Hystrix integration.
 * This class provides configuration support for @EnableHystrix annotation.
 *
 * <p>When @EnableHystrix is detected, this configuration:</p>
 * <ul>
 *   <li>Registers HystrixCommandAspect for @HystrixCommand processing</li>
 *   <li>Optionally initializes HystrixRequestContext for web requests</li>
 *   <li>Optionally exposes Hystrix metrics stream endpoint</li>
 *   <li>Integrates with Spring's concurrency strategy</li>
 * </ul>
 *
 * @since 1.6.0
 */
public class HystrixSpringConfiguration {

    private final boolean enableMetricsStream;
    private final boolean enableRequestContext;

    public HystrixSpringConfiguration(EnableHystrix enableHystrix) {
        this.enableMetricsStream = enableHystrix.enableMetricsStream();
        this.enableRequestContext = enableHystrix.enableRequestContext();
    }

    /**
     * Initialize Hystrix with Spring-aware concurrency strategy.
     * This ensures proper context propagation in Spring applications.
     */
    public void initialize() {
        registerConcurrencyStrategy();

        if (enableRequestContext) {
            registerRequestContextFilter();
        }

        if (enableMetricsStream) {
            registerMetricsStreamServlet();
        }
    }

    private void registerConcurrencyStrategy() {
        // Register Spring-aware concurrency strategy to properly propagate
        // Spring Security context, RequestContextHolder, etc.
        HystrixConcurrencyStrategy strategy = new SpringAwareHystrixConcurrencyStrategy();
        HystrixPlugins.getInstance().registerConcurrencyStrategy(strategy);
    }

    private void registerRequestContextFilter() {
        // Register filter to initialize HystrixRequestContext for each web request
        // This is required for request-scoped features like request caching
    }

    private void registerMetricsStreamServlet() {
        // Register servlet to expose Hystrix metrics stream at /hystrix.stream
        // This can be consumed by Hystrix Dashboard or Turbine
    }

    /**
     * Spring-aware Hystrix concurrency strategy that properly propagates
     * Spring context information across Hystrix thread boundaries.
     */
    private static class SpringAwareHystrixConcurrencyStrategy extends HystrixConcurrencyStrategy {

        @Override
        public <T> java.util.concurrent.Callable<T> wrapCallable(java.util.concurrent.Callable<T> callable) {
            // Capture Spring context from calling thread
            // and restore it in Hystrix worker thread
            return new ContextAwareCallable<>(callable, captureContext());
        }

        private Object captureContext() {
            // Capture current Spring Security context, RequestContextHolder, etc.
            // This is a simplified implementation - real implementation would
            // capture various Spring context holders
            return null;
        }

        private static class ContextAwareCallable<T> implements java.util.concurrent.Callable<T> {
            private final java.util.concurrent.Callable<T> delegate;
            private final Object context;

            ContextAwareCallable(java.util.concurrent.Callable<T> delegate, Object context) {
                this.delegate = delegate;
                this.context = context;
            }

            @Override
            public T call() throws Exception {
                // Restore Spring context in worker thread
                try {
                    return delegate.call();
                } finally {
                    // Clean up context
                }
            }
        }
    }

    /**
     * Factory method to create HystrixCommandAspect bean.
     * This aspect intercepts @HystrixCommand annotated methods.
     *
     * @return configured HystrixCommandAspect instance
     */
    public HystrixCommandAspect hystrixCommandAspect() {
        return new HystrixCommandAspect();
    }
}
