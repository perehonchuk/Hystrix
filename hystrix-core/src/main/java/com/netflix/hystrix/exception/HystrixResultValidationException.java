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
package com.netflix.hystrix.exception;

/**
 * Exception thrown when a command's result fails validation via the isValidResult() method.
 * <p>
 * This exception indicates that while the command executed successfully from a technical perspective
 * (no exception was thrown), the result did not meet the validation criteria defined by the command.
 * <p>
 * When this exception is thrown, Hystrix will treat it as a command failure and attempt to execute
 * the fallback logic, similar to how it handles other execution failures.
 * <p>
 * This allows commands to enforce business-level validation rules on responses and gracefully
 * degrade when those rules are not satisfied.
 */
public class HystrixResultValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public HystrixResultValidationException(String message) {
        super(message);
    }

    public HystrixResultValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
