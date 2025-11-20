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
import java.util.concurrent.ConcurrentHashMap;

import rx.Observable;
import rx.functions.Action1;

import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;

/**
 * Used to wrap code that will execute potentially risky functionality (typically meaning a service call over the network)
 * with fault and latency tolerance, statistics and performance metrics capture, circuit breaker and bulkhead functionality.
 * This command provides a CompletableFuture-based API for modern async applications.
 * <p>
 * This is the third command type in Hystrix, complementing HystrixCommand and HystrixObservableCommand.
 * It provides a CompletableFuture-based execution model that is more aligned with modern Java async patterns.
 *
 * @param <R>
 *            the return type
 *
 * @ThreadSafe
 */
public abstract class HystrixAsyncCommand<R> extends AbstractCommand<R> implements HystrixAsyncExecutable<R>, HystrixInvokableInfo<R>, HystrixObservable<R> {

    /**
     * Construct a {@link HystrixAsyncCommand} with defined {@link HystrixCommandGroupKey}.
     * <p>
     * The {@link HystrixCommandKey} will be derived from the implementing class name.
     *
     * @param group
     *            {@link HystrixCommandGroupKey} used to group together multiple {@link HystrixAsyncCommand} objects.
     *            <p>
     *            The {@link HystrixCommandGroupKey} is used to represent a common relationship between commands. For example, a library or team name, the system all related commands interact with,
     *            common business purpose etc.
     */
    protected HystrixAsyncCommand(HystrixCommandGroupKey group) {
        super(group, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Construct a {@link HystrixAsyncCommand} with defined {@link HystrixCommandGroupKey} and {@link HystrixThreadPoolKey}.
     * <p>
     * The {@link HystrixCommandKey} will be derived from the implementing class name.
     *
     * @param group
     *            {@link HystrixCommandGroupKey} used to group together multiple {@link HystrixAsyncCommand} objects.
     * @param threadPool
     *            {@link HystrixThreadPoolKey} used to identify the thread pool in which a {@link HystrixAsyncCommand} executes.
     */
    protected HystrixAsyncCommand(HystrixCommandGroupKey group, HystrixThreadPoolKey threadPool) {
        super(group, null, threadPool, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Construct a {@link HystrixAsyncCommand} with defined {@link HystrixCommandGroupKey}, {@link HystrixCommandKey}, and {@link HystrixThreadPoolKey}.
     *
     * @param group
     *            {@link HystrixCommandGroupKey} used to group together multiple {@link HystrixAsyncCommand} objects.
     * @param key
     *            {@link HystrixCommandKey} used to identify this command instance for statistics, circuit-breaker, properties, etc.
     * @param threadPool
     *            {@link HystrixThreadPoolKey} used to identify the thread pool in which a {@link HystrixAsyncCommand} executes.
     */
    protected HystrixAsyncCommand(HystrixCommandGroupKey group, HystrixCommandKey key, HystrixThreadPoolKey threadPool) {
        super(group, key, threadPool, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Construct a {@link HystrixAsyncCommand} with defined {@link Setter} that allows injecting property and strategy overrides and other optional arguments.
     * <p>
     * NOTE: The {@link HystrixCommandKey} is used to associate a {@link HystrixAsyncCommand} with {@link HystrixCircuitBreaker}, {@link HystrixCommandMetrics} and other objects.
     * <p>
     * Do not create multiple {@link HystrixCommandKey} objects with the same key but different owners as you may get unpredictable behavior.
     *
     * @param setter
     *            Fluent interface for constructor arguments
     */
    protected HystrixAsyncCommand(Setter setter) {
        super(setter.groupKey, setter.commandKey, setter.threadPoolKey, null, null, setter.commandPropertiesDefaults, setter.threadPoolPropertiesDefaults, null, null, null, null, null);
    }

    /**
     * Implement this method with code to be executed when {@link #executeAsync()} is invoked.
     *
     * @return R response type
     * @throws Exception
     *             if command execution fails
     */
    protected abstract R run() throws Exception;

    /**
     * If {@link #executeAsync()} fails, this method will be invoked to provide a fallback value.
     * <p>
     * By default this returns null, so when fallback support is desired override this method.
     *
     * @return R or throw UnsupportedOperationException if not implemented
     */
    protected R getFallback() {
        throw new UnsupportedOperationException("No fallback available.");
    }

    @Override
    protected String getFallbackMethodName() {
        return "getFallback";
    }

    @Override
    protected boolean isFallbackUserDefined() {
        Boolean containsFromMap = commandContainsFallback.get(commandKey);
        if (containsFromMap != null) {
            return containsFromMap;
        } else {
            Boolean toInsertIntoMap;
            try {
                getClass().getDeclaredMethod("getFallback");
                toInsertIntoMap = true;
            } catch (NoSuchMethodException nsme) {
                toInsertIntoMap = false;
            }
            commandContainsFallback.put(commandKey, toInsertIntoMap);
            return toInsertIntoMap;
        }
    }

    final static ConcurrentHashMap<HystrixCommandKey, Boolean> commandContainsFallback = new ConcurrentHashMap<HystrixCommandKey, Boolean>();

    /**
     * Used for asynchronous execution of command with CompletableFuture.
     * <p>
     * This eagerly starts execution of the command and returns a CompletableFuture
     * that will be completed with the result or completed exceptionally on failure.
     *
     * @return {@code CompletableFuture<R>} that completes with the result of the command execution or a fallback if the command fails
     */
    @Override
    public CompletableFuture<R> executeAsync() {
        final CompletableFuture<R> future = new CompletableFuture<R>();

        // Subscribe to the observable and complete the future when done
        toObservable().subscribe(
            new Action1<R>() {
                @Override
                public void call(R result) {
                    future.complete(result);
                }
            },
            new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    future.completeExceptionally(throwable);
                }
            }
        );

        return future;
    }

    /**
     * Used for synchronous execution of command (blocks until result is available).
     * <p>
     * This is a convenience method that blocks on the CompletableFuture returned by {@link #executeAsync()}.
     *
     * @return R response type
     * @throws HystrixRuntimeException
     *             if an error occurs and a fallback cannot be retrieved
     * @throws HystrixBadRequestException
     *             if invalid arguments or state were used
     */
    public R execute() {
        try {
            return executeAsync().get();
        } catch (Exception e) {
            throw decomposeException(e);
        }
    }

    /**
     * Fluent interface for arguments to the {@link HystrixAsyncCommand} constructor.
     * <p>
     * The required arguments are set via the 'with' factory method and optional arguments via the 'and' chained methods.
     * <p>
     * Example:
     * <pre> {@code
     * Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("GroupName"))
                .andCommandKey(HystrixCommandKey.Factory.asKey("CommandName"))
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("ThreadPoolName"));
     * } </pre>
     *
     * @NotThreadSafe
     */
    public static class Setter {

        protected final HystrixCommandGroupKey groupKey;
        protected HystrixCommandKey commandKey;
        protected HystrixThreadPoolKey threadPoolKey;
        protected HystrixCommandProperties.Setter commandPropertiesDefaults;
        protected HystrixThreadPoolProperties.Setter threadPoolPropertiesDefaults;

        /**
         * Setter factory method containing required values.
         * <p>
         * All optional arguments can be set via the chained methods.
         *
         * @param groupKey
         *            {@link HystrixCommandGroupKey} used to group together multiple {@link HystrixAsyncCommand} objects.
         *            <p>
         *            The {@link HystrixCommandGroupKey} is used to represent a common relationship between commands. For example, a library or team name, the system all related commands interact with,
         *            common business purpose etc.
         */
        protected Setter(HystrixCommandGroupKey groupKey) {
            this.groupKey = groupKey;
        }

        /**
         * Setter factory method with required values.
         * <p>
         * All optional arguments can be set via the chained methods.
         *
         * @param groupKey
         *            {@link HystrixCommandGroupKey} used to group together multiple {@link HystrixAsyncCommand} objects.
         *            <p>
         *            The {@link HystrixCommandGroupKey} is used to represent a common relationship between commands. For example, a library or team name, the system all related commands interact with,
         *            common business purpose etc.
         */
        public static Setter withGroupKey(HystrixCommandGroupKey groupKey) {
            return new Setter(groupKey);
        }

        /**
         * @param commandKey
         *            {@link HystrixCommandKey} used to identify this command instance for statistics, circuit-breaker, properties, etc.
         *            <p>
         *            By default this will be derived from the instance class name.
         *            <p>
         *            NOTE: Every unique {@link HystrixCommandKey} will result in new instances of {@link HystrixCircuitBreaker}, {@link HystrixCommandMetrics} and {@link HystrixCommandProperties}.
         *            Thus,
         *            the number of variants should be kept to a finite and reasonable number to avoid high-memory usage or memory leaks.
         * @return Setter for fluent interface via method chaining
         */
        public Setter andCommandKey(HystrixCommandKey commandKey) {
            this.commandKey = commandKey;
            return this;
        }

        /**
         * @param threadPoolKey
         *            {@link HystrixThreadPoolKey} used to define which thread-pool this command should run on when using thread-isolation.
         *            <p>
         *            By default this is derived from the {@link HystrixCommandGroupKey}.
         * @return Setter for fluent interface via method chaining
         */
        public Setter andThreadPoolKey(HystrixThreadPoolKey threadPoolKey) {
            this.threadPoolKey = threadPoolKey;
            return this;
        }

        /**
         * @param commandPropertiesDefaults
         *            {@link HystrixCommandProperties.Setter} with property overrides for this specific instance of {@link HystrixAsyncCommand}.
         *            <p>
         *            See the {@link HystrixPropertiesStrategy} JavaDocs for more information on properties and order of precedence.
         * @return Setter for fluent interface via method chaining
         */
        public Setter andCommandPropertiesDefaults(HystrixCommandProperties.Setter commandPropertiesDefaults) {
            this.commandPropertiesDefaults = commandPropertiesDefaults;
            return this;
        }

        /**
         * @param threadPoolPropertiesDefaults
         *            {@link HystrixThreadPoolProperties.Setter} with property overrides for the {@link HystrixThreadPool} used by this specific instance of {@link HystrixAsyncCommand}.
         *            <p>
         *            See the {@link HystrixPropertiesStrategy} JavaDocs for more information on properties and order of precedence.
         * @return Setter for fluent interface via method chaining
         */
        public Setter andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter threadPoolPropertiesDefaults) {
            this.threadPoolPropertiesDefaults = threadPoolPropertiesDefaults;
            return this;
        }

    }
}
