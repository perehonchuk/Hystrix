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
 * Example demonstrating circuit breaker warmup phase functionality.
 *
 * When warmup is enabled, the circuit breaker gradually ramps up traffic after recovery
 * instead of immediately allowing 100% of requests. This helps prevent overwhelming
 * a recently recovered service.
 *
 * Configuration:
 * - warmup.enabled: Enable/disable warmup phase (default: true)
 * - warmup.durationInMilliseconds: How long warmup should last (default: 10000ms)
 * - warmup.minTrafficPercentage: Starting traffic percentage (default: 25%)
 * - warmup.requestThreshold: Successful requests needed to complete warmup (default: 20)
 */
public class CommandWithWarmupPhase extends HystrixCommand<String> {

    private final String name;

    public CommandWithWarmupPhase(String name) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withCircuitBreakerWarmupEnabled(true)
                        .withCircuitBreakerWarmupDurationInMilliseconds(10000)
                        .withCircuitBreakerWarmupMinTrafficPercentage(25)
                        .withCircuitBreakerWarmupRequestThreshold(20)));
        this.name = name;
    }

    @Override
    protected String run() {
        return "Hello " + name + "!";
    }
}
