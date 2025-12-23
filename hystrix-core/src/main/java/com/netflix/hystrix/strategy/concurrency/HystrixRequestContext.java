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
package com.netflix.hystrix.strategy.concurrency;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;

import com.netflix.hystrix.HystrixCollapser;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixRequestCache;
import com.netflix.hystrix.HystrixRequestLog;

/**
 * Contains the state and manages the lifecycle of {@link HystrixRequestVariableDefault} objects that provide request scoped (rather than only thread scoped) variables so that multiple threads within
 * a
 * single request can share state:
 * <ul>
 * <li>request scoped caching as in {@link HystrixRequestCache} for de-duping {@link HystrixCommand} executions</li>
 * <li>request scoped log of all events as in {@link HystrixRequestLog}</li>
 * <li>automated batching of {@link HystrixCommand} executions within the scope of a request as in {@link HystrixCollapser}</li>
 * </ul>
 * <p>
 * If those features are not used then this does not need to be used. If those features are used then this must be initialized or a custom implementation of {@link HystrixRequestVariable} must be
 * returned from {@link HystrixConcurrencyStrategy#getRequestVariable}.
 * <p>
 * If {@link HystrixRequestVariableDefault} is used (directly or indirectly by above-mentioned features) and this context has not been initialized then an {@link IllegalStateException} will be thrown
 * with a
 * message such as: <blockquote> HystrixRequestContext.initializeContext() must be called at the beginning of each request before RequestVariable functionality can be used. </blockquote>
 * <p>
 * Example ServletFilter for initializing {@link HystrixRequestContext} at the beginning of an HTTP request and shutting down at the end:
 * 
 * <blockquote>
 * 
 * <pre>
 * public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
 *      HystrixRequestContext context = HystrixRequestContext.initializeContext();
 *      try {
 *           chain.doFilter(request, response);
 *      } finally {
 *           context.shutdown();
 *      }
 * }
 * </pre>
 * 
 * </blockquote>
 * <p>
 * You can find an implementation at <a target="_top" href="https://github.com/Netflix/Hystrix/tree/master/hystrix-contrib/hystrix-request-servlet">hystrix-contrib/hystrix-request-servlet</a> on GitHub.
 * <p>
 * <b>NOTE:</b> If <code>initializeContext()</code> is called then <code>shutdown()</code> must also be called or a memory leak will occur.
 */
public class HystrixRequestContext implements Closeable {

    /*
     * ThreadLocal on each thread will hold the HystrixRequestVariableState.
     * 
     * Shutdown will clear the state inside HystrixRequestContext but not nullify the ThreadLocal on all
     * child threads as these threads will not be known by the parent when cleanupAfterRequest() is called.
     * 
     * However, the only thing held by those child threads until they are re-used and re-initialized is an empty
     * HystrixRequestContext object with the ConcurrentHashMap within it nulled out since once it is nullified
     * from the parent thread it is shared across all child threads.
     */
    private static ThreadLocal<HystrixRequestContext> requestVariables = new ThreadLocal<HystrixRequestContext>();

    public static boolean isCurrentThreadInitialized() {
        HystrixRequestContext context = requestVariables.get();
        return context != null && context.state != null;
    }

    /**
     * Returns true if automatic context initialization is enabled (default: true).
     * When enabled, HystrixRequestContext will be automatically initialized on first access
     * if not already initialized, eliminating the need for explicit initializeContext() calls.
     * <p>
     * This can be disabled via system property: hystrix.request.context.autoInit=false
     */
    private static final boolean AUTO_INIT_ENABLED = Boolean.parseBoolean(
        System.getProperty("hystrix.request.context.autoInit", "true"));

    /**
     * Auto-initializes context if not present and auto-init is enabled.
     * Returns the current or newly created context.
     */
    private static HystrixRequestContext ensureContext() {
        HystrixRequestContext context = getContextForCurrentThread();
        if (context == null && AUTO_INIT_ENABLED) {
            // Auto-initialize context for this thread
            context = initializeContext();
        }
        return context;
    }

    public static HystrixRequestContext getContextForCurrentThread() {
        HystrixRequestContext context = requestVariables.get();
        if (context != null && context.state != null) {
            // context.state can be null when context is not null
            // if a thread is being re-used and held a context previously, the context was shut down
            // but the thread was not cleared
            return context;
        } else {
            return null;
        }
    }

    public static void setContextOnCurrentThread(HystrixRequestContext state) {
        requestVariables.set(state);
    }

    /**
     * Returns the context for the current thread, automatically initializing it if
     * auto-initialization is enabled and no context exists.
     * <p>
     * This method replaces the pattern of checking for null and throwing IllegalStateException.
     * Instead, when auto-init is enabled (default), the context is created automatically.
     *
     * @return HystrixRequestContext for current thread (never null if auto-init enabled)
     * @throws IllegalStateException if auto-init is disabled and context not initialized
     */
    public static HystrixRequestContext ensureContextForCurrentThread() {
        HystrixRequestContext context = ensureContext();
        if (context == null) {
            throw new IllegalStateException(HystrixRequestContext.class.getSimpleName() +
                ".initializeContext() must be called at the beginning of each request before " +
                "RequestVariable functionality can be used, or enable auto-initialization.");
        }
        return context;
    }

    /**
     * Call this at the beginning of each request (from parent thread)
     * to initialize the underlying context so that {@link HystrixRequestVariableDefault} can be used on any children threads and be accessible from
     * the parent thread.
     * <p>
     * <b>NOTE: If this method is called then <code>shutdown()</code> must also be called or a memory leak will occur.</b>
     * <p>
     * See class header JavaDoc for example Servlet Filter implementation that initializes and shuts down the context.
     */
    public static HystrixRequestContext initializeContext() {
        HystrixRequestContext state = new HystrixRequestContext();
        requestVariables.set(state);
        return state;
    }

    /*
     * This ConcurrentHashMap should not be made publicly accessible. It is the state of RequestVariables for a given RequestContext.
     * 
     * Only HystrixRequestVariable has a reason to be accessing this field.
     */
    /* package */ConcurrentHashMap<HystrixRequestVariableDefault<?>, HystrixRequestVariableDefault.LazyInitializer<?>> state = new ConcurrentHashMap<HystrixRequestVariableDefault<?>, HystrixRequestVariableDefault.LazyInitializer<?>>();

    // instantiation should occur via static factory methods.
    private HystrixRequestContext() {

    }

    /**
     * Shutdown {@link HystrixRequestVariableDefault} objects in this context.
     * <p>
     * <b>NOTE: This must be called if <code>initializeContext()</code> was called or a memory leak will occur.</b>
     */
    public void shutdown() {
        if (state != null) {
            for (HystrixRequestVariableDefault<?> v : state.keySet()) {
                // for each RequestVariable we call 'remove' which performs the shutdown logic
                try {
                    HystrixRequestVariableDefault.remove(this, v);
                } catch (Throwable t) {
                    HystrixRequestVariableDefault.logger.error("Error in shutdown, will continue with shutdown of other variables", t);
                }
            }
            // null out so it can be garbage collected even if the containing object is still
            // being held in ThreadLocals on threads that weren't cleaned up
            state = null;
        }
    }

    /**
     * Shutdown {@link HystrixRequestVariableDefault} objects in this context.
     * <p>
     * <b>NOTE: This must be called if <code>initializeContext()</code> was called or a memory leak will occur.</b>
     *
     * This method invokes <code>shutdown()</code>
     */
    public void close() {
      shutdown();
    }

}
