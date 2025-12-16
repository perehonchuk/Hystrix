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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;

/**
 * Sample {@link HystrixCommand} showing automatic retry before fallback.
 * The command will retry execution up to 2 times (default) before falling back.
 */
public class CommandWithRetry extends HystrixCommand<String> {

    private final String name;
    private final AtomicInteger attemptCount;

    public CommandWithRetry(String name, AtomicInteger attemptCount) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionRetryCount(2)
                        .withExecutionRetryDelayInMilliseconds(100)));
        this.name = name;
        this.attemptCount = attemptCount;
    }

    @Override
    protected String run() {
        int currentAttempt = attemptCount.incrementAndGet();
        System.out.println("Execution attempt: " + currentAttempt);
        throw new RuntimeException("this command always fails on attempt " + currentAttempt);
    }

    @Override
    protected String getFallback() {
        return "Hello Fallback " + name + " after " + attemptCount.get() + " attempts!";
    }

    public static class UnitTest {

        @Test
        public void testSynchronous() {
            AtomicInteger attempts = new AtomicInteger(0);
            String result = new CommandWithRetry("World", attempts).execute();
            // Should have tried 3 times total (1 initial + 2 retries) before falling back
            assertEquals(3, attempts.get());
            assertEquals("Hello Fallback World after 3 attempts!", result);
        }

        @Test
        public void testAsynchronous() throws Exception {
            AtomicInteger attempts = new AtomicInteger(0);
            Future<String> fWorld = new CommandWithRetry("World", attempts).queue();
            String result = fWorld.get();
            // Should have tried 3 times total (1 initial + 2 retries) before falling back
            assertEquals(3, attempts.get());
            assertEquals("Hello Fallback World after 3 attempts!", result);
        }
    }

}
