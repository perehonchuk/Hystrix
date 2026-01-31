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
 * An exception representing a response validation failure.
 * <p>
 * When response validation is enabled, this exception is thrown when the response from {@link HystrixCommand#run()}
 * or {@link com.netflix.hystrix.HystrixObservableCommand#construct()} fails the validation criteria defined in
 * {@link HystrixCommand#validateResponse(Object)} or {@link com.netflix.hystrix.HystrixObservableCommand#validateResponse(Object)}.
 * <p>
 * This exception triggers fallback logic and counts against failure metrics, potentially affecting circuit breaker decisions.
 * <p>
 * Response validation allows commands to define business-level success criteria beyond just execution success.
 * For example, a command might complete without throwing an exception but return a response indicating an invalid
 * state (null value, empty collection, error code in response object, etc.). The validation mechanism allows treating
 * such responses as failures.
 */
public class HystrixResponseValidationException extends RuntimeException {

    private static final long serialVersionUID = -2349872839472838472L;

    private final Object invalidResponse;

    public HystrixResponseValidationException(String message, Object invalidResponse) {
        super(message);
        this.invalidResponse = invalidResponse;
    }

    public HystrixResponseValidationException(String message, Object invalidResponse, Throwable cause) {
        super(message, cause);
        this.invalidResponse = invalidResponse;
    }

    /**
     * Get the response value that failed validation.
     *
     * @return the invalid response object
     */
    public Object getInvalidResponse() {
        return invalidResponse;
    }
}
