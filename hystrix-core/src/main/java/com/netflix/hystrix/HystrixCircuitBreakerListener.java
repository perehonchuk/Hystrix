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

/**
 * Listener interface for receiving notifications about circuit breaker state transitions.
 * <p>
 * Implementations of this interface can be registered with a {@link HystrixCircuitBreaker} to receive
 * real-time notifications when the circuit transitions between states (CLOSED, OPEN, HALF_OPEN).
 * <p>
 * This is useful for:
 * <ul>
 * <li>Monitoring and alerting on circuit breaker state changes</li>
 * <li>Logging circuit breaker transitions for debugging</li>
 * <li>Triggering custom recovery logic or notifications when circuits open</li>
 * <li>Collecting metrics about circuit breaker behavior</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * HystrixCircuitBreakerListener listener = new HystrixCircuitBreakerListener() {
 *     public void onCircuitOpened(HystrixCommandKey key) {
 *         logger.warn("Circuit opened for command: " + key.name());
 *         alertingSystem.sendAlert("Circuit breaker opened", key.name());
 *     }
 *
 *     public void onCircuitClosed(HystrixCommandKey key) {
 *         logger.info("Circuit closed for command: " + key.name());
 *     }
 * };
 *
 * HystrixCircuitBreaker.Factory.registerListener(commandKey, listener);
 * </pre>
 */
public interface HystrixCircuitBreakerListener {

    /**
     * Called when the circuit breaker transitions from CLOSED to OPEN state.
     * <p>
     * This occurs when the error rate exceeds the configured threshold.
     *
     * @param commandKey The key of the command whose circuit breaker opened
     */
    void onCircuitOpened(HystrixCommandKey commandKey);

    /**
     * Called when the circuit breaker transitions from OPEN to HALF_OPEN state.
     * <p>
     * This occurs when the sleep window elapses and the circuit breaker allows
     * a test request through to check if the underlying service has recovered.
     *
     * @param commandKey The key of the command whose circuit breaker entered half-open state
     */
    void onCircuitHalfOpen(HystrixCommandKey commandKey);

    /**
     * Called when the circuit breaker transitions from HALF_OPEN to CLOSED state.
     * <p>
     * This occurs when the test request(s) in half-open state succeed, indicating
     * that the underlying service has recovered.
     *
     * @param commandKey The key of the command whose circuit breaker closed
     */
    void onCircuitClosed(HystrixCommandKey commandKey);

    /**
     * Called when the circuit breaker transitions from HALF_OPEN back to OPEN state.
     * <p>
     * This occurs when the test request(s) in half-open state fail, indicating
     * that the underlying service has not yet recovered.
     *
     * @param commandKey The key of the command whose circuit breaker re-opened
     */
    void onCircuitReopened(HystrixCommandKey commandKey);
}
