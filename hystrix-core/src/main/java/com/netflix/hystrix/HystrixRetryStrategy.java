/**
 * Copyright 2024 Netflix, Inc.
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
 * Retry strategies that determine how and when commands should be retried before falling back.
 * <p>
 * When command execution fails, Hystrix can now automatically retry the operation
 * based on the configured strategy before invoking fallback logic or failing.
 */
public enum HystrixRetryStrategy {

    /**
     * No retries - fail immediately on first error (default behavior).
     * This maintains backward compatibility with existing Hystrix behavior.
     */
    NONE {
        @Override
        public long getDelayMillis(int attemptNumber) {
            return 0;
        }
    },

    /**
     * Retry immediately without any delay between attempts.
     * Useful for transient network glitches or temporary resource contention.
     */
    IMMEDIATE {
        @Override
        public long getDelayMillis(int attemptNumber) {
            return 0;
        }
    },

    /**
     * Exponential backoff - delay doubles after each retry attempt.
     * Pattern: 100ms, 200ms, 400ms, 800ms, 1600ms, ...
     * Useful for rate-limited APIs or services under load.
     */
    EXPONENTIAL_BACKOFF {
        @Override
        public long getDelayMillis(int attemptNumber) {
            // Base delay of 100ms, doubled for each attempt
            return Math.min(100L * (1L << attemptNumber), 30000L); // Cap at 30 seconds
        }
    },

    /**
     * Linear backoff - fixed delay between each retry attempt.
     * Pattern: 200ms, 200ms, 200ms, ...
     * Useful for predictable retry patterns.
     */
    LINEAR_BACKOFF {
        @Override
        public long getDelayMillis(int attemptNumber) {
            return 200L;
        }
    },

    /**
     * Fibonacci backoff - delay follows Fibonacci sequence.
     * Pattern: 100ms, 100ms, 200ms, 300ms, 500ms, 800ms, ...
     * Provides a middle ground between linear and exponential.
     */
    FIBONACCI_BACKOFF {
        @Override
        public long getDelayMillis(int attemptNumber) {
            return Math.min(fibonacciDelay(attemptNumber), 30000L); // Cap at 30 seconds
        }

        private long fibonacciDelay(int n) {
            if (n <= 1) return 100L;
            long a = 100L, b = 100L;
            for (int i = 2; i <= n; i++) {
                long temp = a + b;
                a = b;
                b = temp;
            }
            return b;
        }
    };

    /**
     * Calculate the delay in milliseconds before the next retry attempt.
     *
     * @param attemptNumber The retry attempt number (0 for first retry, 1 for second, etc.)
     * @return Delay in milliseconds before next retry
     */
    public abstract long getDelayMillis(int attemptNumber);

    /**
     * Check if this strategy performs retries.
     *
     * @return true if strategy retries, false otherwise
     */
    public boolean shouldRetry() {
        return this != NONE;
    }
}
