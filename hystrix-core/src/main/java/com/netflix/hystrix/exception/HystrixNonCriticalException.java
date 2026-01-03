/**
 * Copyright 2012 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.hystrix.exception;

import com.netflix.hystrix.HystrixCommand;

/**
 * An exception representing a non-critical failure that should not trigger fallback logic.
 * <p>
 * Unlike regular exceptions thrown by a {@link HystrixCommand}, this will propagate directly to the caller
 * without triggering fallback execution. However, unlike {@link HystrixBadRequestException}, this WILL count
 * against failure metrics and can contribute to triggering the circuit breaker.
 * <p>
 * This exception type is useful for scenarios where:
 * <ul>
 * <li>The failure should be tracked and can open the circuit breaker</li>
 * <li>The fallback behavior would not provide value (e.g., certain validation failures)</li>
 * <li>The caller needs to handle the specific error condition directly</li>
 * </ul>
 */
public class HystrixNonCriticalException extends RuntimeException {

    private static final long serialVersionUID = -8765432103561805857L;

    public HystrixNonCriticalException(String message) {
        super(message);
    }

    public HystrixNonCriticalException(String message, Throwable cause) {
        super(message, cause);
    }
}
