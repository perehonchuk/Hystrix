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

import static org.junit.Assert.*;

import java.util.concurrent.Future;

import org.junit.Test;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

/**
 * Sample {@link HystrixCommand} showing a secondary fallback implementation.
 * This demonstrates the new multi-tier fallback capability where a secondary
 * fallback is invoked if the primary fallback fails.
 */
public class CommandWithSecondaryFallback extends HystrixCommand<String> {

    private final String name;
    private final boolean primaryFallbackShouldFail;

    public CommandWithSecondaryFallback(String name, boolean primaryFallbackShouldFail) {
        super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"));
        this.name = name;
        this.primaryFallbackShouldFail = primaryFallbackShouldFail;
    }

    @Override
    protected String run() {
        throw new RuntimeException("this command always fails");
    }

    @Override
    protected String getFallback() {
        if (primaryFallbackShouldFail) {
            throw new RuntimeException("primary fallback also fails");
        }
        return "Primary Fallback " + name + "!";
    }

    @Override
    protected String getSecondaryFallback() {
        return "Secondary Fallback " + name + "!";
    }

    public static class UnitTest {

        @Test
        public void testPrimaryFallbackSuccess() {
            assertEquals("Primary Fallback World!", new CommandWithSecondaryFallback("World", false).execute());
        }

        @Test
        public void testSecondaryFallbackWhenPrimaryFails() {
            assertEquals("Secondary Fallback World!", new CommandWithSecondaryFallback("World", true).execute());
        }

        @Test
        public void testAsynchronousSecondaryFallback() throws Exception {
            Future<String> result = new CommandWithSecondaryFallback("Bob", true).queue();
            assertEquals("Secondary Fallback Bob!", result.get());
        }
    }

}
