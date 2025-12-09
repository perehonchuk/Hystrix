/**
 * Copyright 2013 Netflix, Inc.
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
package com.netflix.hystrix.strategy.executionhook;

import com.netflix.hystrix.HystrixInvokable;
import com.netflix.hystrix.exception.HystrixRuntimeException.FailureType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Composite implementation of {@link HystrixCommandExecutionHook} that chains multiple hooks together.
 * Hooks are executed in the order they were registered.
 *
 * @ExcludeFromJavadoc
 */
public class HystrixCommandExecutionHookChain extends HystrixCommandExecutionHook {

    private final List<HystrixCommandExecutionHook> hooks;

    public HystrixCommandExecutionHookChain(List<HystrixCommandExecutionHook> hooks) {
        this.hooks = Collections.unmodifiableList(new ArrayList<HystrixCommandExecutionHook>(hooks));
    }

    @Override
    public <T> void onStart(HystrixInvokable<T> commandInstance) {
        for (HystrixCommandExecutionHook hook : hooks) {
            hook.onStart(commandInstance);
        }
    }

    @Override
    public <T> T onEmit(HystrixInvokable<T> commandInstance, T value) {
        T result = value;
        for (HystrixCommandExecutionHook hook : hooks) {
            result = hook.onEmit(commandInstance, result);
        }
        return result;
    }

    @Override
    public <T> Exception onError(HystrixInvokable<T> commandInstance, FailureType failureType, Exception e) {
        Exception result = e;
        for (HystrixCommandExecutionHook hook : hooks) {
            result = hook.onError(commandInstance, failureType, result);
        }
        return result;
    }

    @Override
    public <T> void onSuccess(HystrixInvokable<T> commandInstance) {
        for (HystrixCommandExecutionHook hook : hooks) {
            hook.onSuccess(commandInstance);
        }
    }

    @Override
    public <T> void onThreadStart(HystrixInvokable<T> commandInstance) {
        for (HystrixCommandExecutionHook hook : hooks) {
            hook.onThreadStart(commandInstance);
        }
    }

    @Override
    public <T> void onThreadComplete(HystrixInvokable<T> commandInstance) {
        for (HystrixCommandExecutionHook hook : hooks) {
            hook.onThreadComplete(commandInstance);
        }
    }

    @Override
    public <T> void onExecutionStart(HystrixInvokable<T> commandInstance) {
        for (HystrixCommandExecutionHook hook : hooks) {
            hook.onExecutionStart(commandInstance);
        }
    }

    @Override
    public <T> T onExecutionEmit(HystrixInvokable<T> commandInstance, T value) {
        T result = value;
        for (HystrixCommandExecutionHook hook : hooks) {
            result = hook.onExecutionEmit(commandInstance, result);
        }
        return result;
    }

    @Override
    public <T> Exception onExecutionError(HystrixInvokable<T> commandInstance, Exception e) {
        Exception result = e;
        for (HystrixCommandExecutionHook hook : hooks) {
            result = hook.onExecutionError(commandInstance, result);
        }
        return result;
    }

    @Override
    public <T> void onExecutionSuccess(HystrixInvokable<T> commandInstance) {
        for (HystrixCommandExecutionHook hook : hooks) {
            hook.onExecutionSuccess(commandInstance);
        }
    }

    @Override
    public <T> void onFallbackStart(HystrixInvokable<T> commandInstance) {
        for (HystrixCommandExecutionHook hook : hooks) {
            hook.onFallbackStart(commandInstance);
        }
    }

    @Override
    public <T> T onFallbackEmit(HystrixInvokable<T> commandInstance, T value) {
        T result = value;
        for (HystrixCommandExecutionHook hook : hooks) {
            result = hook.onFallbackEmit(commandInstance, result);
        }
        return result;
    }

    @Override
    public <T> Exception onFallbackError(HystrixInvokable<T> commandInstance, Exception e) {
        Exception result = e;
        for (HystrixCommandExecutionHook hook : hooks) {
            result = hook.onFallbackError(commandInstance, result);
        }
        return result;
    }

    @Override
    public <T> void onFallbackSuccess(HystrixInvokable<T> commandInstance) {
        for (HystrixCommandExecutionHook hook : hooks) {
            hook.onFallbackSuccess(commandInstance);
        }
    }

    @Override
    public <T> void onCacheHit(HystrixInvokable<T> commandInstance) {
        for (HystrixCommandExecutionHook hook : hooks) {
            hook.onCacheHit(commandInstance);
        }
    }

    @Override
    public <T> void onUnsubscribe(HystrixInvokable<T> commandInstance) {
        for (HystrixCommandExecutionHook hook : hooks) {
            hook.onUnsubscribe(commandInstance);
        }
    }

    public List<HystrixCommandExecutionHook> getHooks() {
        return hooks;
    }
}
