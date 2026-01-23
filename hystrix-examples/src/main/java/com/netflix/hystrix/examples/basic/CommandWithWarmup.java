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
package com.netflix.hystrix.examples.basic;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;

/**
 * Example of a {@link HystrixCommand} that uses warmup to initialize state before execution.
 * The warmup phase allows the command to perform cache warming, health checks, or other
 * initialization work before processing real requests.
 */
public class CommandWithWarmup extends HystrixCommand<String> {

    private final String serviceUrl;
    private volatile boolean cacheWarmed = false;

    public CommandWithWarmup(String serviceUrl) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withWarmupEnabled(true)
                        .withWarmupTimeoutInMilliseconds(1000)));
        this.serviceUrl = serviceUrl;
    }

    @Override
    protected void warmup() throws Exception {
        // Perform warmup activities like cache warming, connection pool initialization, etc.
        System.out.println("Warming up service connection to: " + serviceUrl);
        Thread.sleep(100); // Simulate warmup work
        cacheWarmed = true;
        System.out.println("Warmup completed successfully");
    }

    @Override
    protected String run() throws Exception {
        // Execute the actual service call
        String status = cacheWarmed ? " (cache warmed)" : " (no warmup)";
        return "Service response from " + serviceUrl + status;
    }

    @Override
    protected String getFallback() {
        return "Fallback response for " + serviceUrl;
    }
}
