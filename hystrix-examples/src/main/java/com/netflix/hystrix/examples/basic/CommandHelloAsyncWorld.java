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

import com.netflix.hystrix.HystrixAsyncCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

import java.util.concurrent.CompletableFuture;

/**
 * Sample {@link HystrixAsyncCommand} pattern using CompletableFuture-based execution.
 * <p>
 * This demonstrates the new async command type that provides a modern alternative to
 * Observable-based commands.
 */
public class CommandHelloAsyncWorld extends HystrixAsyncCommand<String> {

    private final String name;

    public CommandHelloAsyncWorld(String name) {
        super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"));
        this.name = name;
    }

    @Override
    protected String run() {
        // a real example would do work like a network call here
        return "Hello " + name + " (async)!";
    }

    @Override
    protected String getFallback() {
        return "Hello Fallback (async)";
    }

    public static void main(String[] args) throws Exception {
        // Execute synchronously (blocks until complete)
        String s = new CommandHelloAsyncWorld("World").execute();
        System.out.println("Sync result: " + s);

        // Execute asynchronously with CompletableFuture
        CompletableFuture<String> future = new CommandHelloAsyncWorld("Async World").executeAsync();
        System.out.println("Async operation started...");

        // Chain operations on the CompletableFuture
        future.thenAccept(result -> {
            System.out.println("Async result: " + result);
        }).exceptionally(throwable -> {
            System.err.println("Async error: " + throwable.getMessage());
            return null;
        });

        // Wait for async operation to complete
        Thread.sleep(1000);
    }
}
