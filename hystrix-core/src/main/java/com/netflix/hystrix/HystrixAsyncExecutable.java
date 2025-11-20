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
package com.netflix.hystrix;

import java.util.concurrent.CompletableFuture;

import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;

/**
 * Interface for async executables that use CompletableFuture-based execution.
 * This provides a modern async API alternative to Observable-based commands.
 *
 * @param <R>
 */
public interface HystrixAsyncExecutable<R> extends HystrixInvokable<R> {

    /**
     * Used for asynchronous execution of command with CompletableFuture.
     * <p>
     * This eagerly starts execution of the command and returns a CompletableFuture
     * that will be completed with the result or completed exceptionally on failure.
     * <p>
     * This provides a modern alternative to Observable-based execution for applications
     * that prefer CompletableFuture API.
     *
     * @return {@code CompletableFuture<R>} that completes with the result of the command execution or a fallback if the command fails
     * @throws HystrixRuntimeException
     *             if an error occurs and a fallback cannot be retrieved
     * @throws HystrixBadRequestException
     *             if the {@link HystrixAsyncCommand} instance considers request arguments to be invalid
     */
    public CompletableFuture<R> executeAsync();

}
