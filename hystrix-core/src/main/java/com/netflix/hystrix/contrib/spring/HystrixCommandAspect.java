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

import com.netflix.hystrix.*;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import rx.Observable;

import java.lang.reflect.Method;
import java.util.concurrent.Future;

/**
 * AspectJ aspect for processing @HystrixCommand annotations.
 * This provides native Spring AOP integration without requiring Spring Cloud.
 *
 * <p>To enable this aspect in your Spring application, add the following configuration:</p>
 * <pre>
 * {@literal @}Configuration
 * {@literal @}EnableAspectJAutoProxy
 * public class HystrixConfiguration {
 *     {@literal @}Bean
 *     public HystrixCommandAspect hystrixCommandAspect() {
 *         return new HystrixCommandAspect();
 *     }
 * }
 * </pre>
 *
 * @since 1.6.0
 */
public class HystrixCommandAspect {

    /**
     * Process methods annotated with @HystrixCommand.
     * This method wraps the target method execution in a Hystrix command.
     *
     * @param method the method being invoked
     * @param args method arguments
     * @param target target object
     * @param annotation the HystrixCommand annotation
     * @return result of method execution or fallback
     * @throws Throwable if execution fails and no fallback available
     */
    public Object processHystrixCommand(Method method, Object[] args, Object target,
                                       HystrixCommand annotation) throws Throwable {

        String commandKey = getCommandKey(annotation, method);
        String groupKey = getGroupKey(annotation, target);
        String threadPoolKey = getThreadPoolKey(annotation, groupKey);

        HystrixCommand.Setter setter = HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey))
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(threadPoolKey))
                .andCommandPropertiesDefaults(buildCommandProperties(annotation))
                .andThreadPoolPropertiesDefaults(buildThreadPoolProperties(annotation));

        GenericHystrixCommand hystrixCommand = new GenericHystrixCommand(
                setter, method, args, target, annotation.fallbackMethod());

        return executeCommand(hystrixCommand, method.getReturnType());
    }

    private String getCommandKey(HystrixCommand annotation, Method method) {
        if (annotation.commandKey().isEmpty()) {
            return method.getName();
        }
        return annotation.commandKey();
    }

    private String getGroupKey(HystrixCommand annotation, Object target) {
        if (annotation.groupKey().isEmpty()) {
            return target.getClass().getSimpleName();
        }
        return annotation.groupKey();
    }

    private String getThreadPoolKey(HystrixCommand annotation, String defaultKey) {
        if (annotation.threadPoolKey().isEmpty()) {
            return defaultKey;
        }
        return annotation.threadPoolKey();
    }

    private HystrixCommandProperties.Setter buildCommandProperties(HystrixCommand annotation) {
        HystrixCommandProperties.Setter properties = HystrixCommandProperties.Setter();

        properties.withExecutionTimeoutInMilliseconds(annotation.timeoutInMilliseconds());
        properties.withCircuitBreakerErrorThresholdPercentage(annotation.errorThresholdPercentage());
        properties.withCircuitBreakerRequestVolumeThreshold(annotation.requestVolumeThreshold());
        properties.withCircuitBreakerSleepWindowInMilliseconds(annotation.sleepWindowInMilliseconds());
        properties.withRequestCacheEnabled(annotation.requestCacheEnabled());

        if (annotation.isolationStrategy() == IsolationStrategy.THREAD) {
            properties.withExecutionIsolationStrategy(
                    HystrixCommandProperties.ExecutionIsolationStrategy.THREAD);
        } else {
            properties.withExecutionIsolationStrategy(
                    HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE);
            properties.withExecutionIsolationSemaphoreMaxConcurrentRequests(
                    annotation.maxConcurrentRequests());
        }

        return properties;
    }

    private HystrixThreadPoolProperties.Setter buildThreadPoolProperties(HystrixCommand annotation) {
        return HystrixThreadPoolProperties.Setter()
                .withCoreSize(annotation.corePoolSize());
    }

    private Object executeCommand(GenericHystrixCommand command, Class<?> returnType)
            throws Throwable {

        if (returnType.equals(Observable.class)) {
            return command.observe();
        } else if (returnType.equals(Future.class)) {
            return command.queue();
        } else if (returnType.equals(void.class) || returnType.equals(Void.class)) {
            command.execute();
            return null;
        } else {
            return command.execute();
        }
    }

    /**
     * Internal Hystrix command wrapper for generic method execution.
     */
    private static class GenericHystrixCommand extends com.netflix.hystrix.HystrixCommand<Object> {

        private final Method method;
        private final Object[] args;
        private final Object target;
        private final String fallbackMethodName;

        protected GenericHystrixCommand(Setter setter, Method method, Object[] args,
                                      Object target, String fallbackMethodName) {
            super(setter);
            this.method = method;
            this.args = args;
            this.target = target;
            this.fallbackMethodName = fallbackMethodName;
        }

        @Override
        protected Object run() throws Exception {
            try {
                method.setAccessible(true);
                return method.invoke(target, args);
            } catch (Exception e) {
                throw new HystrixRuntimeException(
                        FailureType.COMMAND_EXCEPTION,
                        this.getClass(),
                        "Failed to execute method: " + method.getName(),
                        e,
                        null);
            }
        }

        @Override
        protected Object getFallback() {
            if (fallbackMethodName == null || fallbackMethodName.isEmpty()) {
                return super.getFallback();
            }

            try {
                Method fallbackMethod = target.getClass()
                        .getDeclaredMethod(fallbackMethodName, method.getParameterTypes());
                fallbackMethod.setAccessible(true);
                return fallbackMethod.invoke(target, args);
            } catch (Exception e) {
                throw new HystrixRuntimeException(
                        FailureType.COMMAND_EXCEPTION,
                        this.getClass(),
                        "Failed to execute fallback method: " + fallbackMethodName,
                        e,
                        null);
            }
        }
    }
}
