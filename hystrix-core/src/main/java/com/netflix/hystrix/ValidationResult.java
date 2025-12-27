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
 * Represents the result of request validation performed before command execution.
 * <p>
 * Commands can return a ValidationResult from the validateRequest() method to indicate
 * that the request should not be executed. This allows early rejection of invalid requests
 * before consuming execution resources (thread pool, semaphore) or affecting circuit breaker metrics.
 *
 * @ThreadSafe
 */
public class ValidationResult {
    private final String errorMessage;
    private final ValidationFailureType failureType;

    /**
     * Types of validation failures that can occur.
     */
    public enum ValidationFailureType {
        /**
         * Invalid request parameters (null, out of range, malformed, etc.)
         */
        INVALID_PARAMETERS,

        /**
         * Request violates business rules or constraints
         */
        BUSINESS_RULE_VIOLATION,

        /**
         * Required dependencies or resources are not available
         */
        MISSING_DEPENDENCY,

        /**
         * Generic validation failure
         */
        GENERAL_VALIDATION_FAILURE
    }

    /**
     * Creates a validation failure result.
     *
     * @param errorMessage description of the validation failure
     * @param failureType type of validation failure
     */
    public ValidationResult(String errorMessage, ValidationFailureType failureType) {
        this.errorMessage = errorMessage;
        this.failureType = failureType;
    }

    /**
     * Creates a validation failure with type GENERAL_VALIDATION_FAILURE.
     *
     * @param errorMessage description of the validation failure
     */
    public ValidationResult(String errorMessage) {
        this(errorMessage, ValidationFailureType.GENERAL_VALIDATION_FAILURE);
    }

    /**
     * @return the validation error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @return the type of validation failure
     */
    public ValidationFailureType getFailureType() {
        return failureType;
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "failureType=" + failureType +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
