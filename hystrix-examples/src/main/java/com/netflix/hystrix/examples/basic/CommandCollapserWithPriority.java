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
package com.netflix.hystrix.examples.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.netflix.hystrix.HystrixCollapser;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;

/**
 * Sample {@link HystrixCollapser} that demonstrates priority-based batching.
 * Requests are grouped by priority level before execution, allowing higher-priority
 * requests to be batched and executed separately from lower-priority requests.
 */
public class CommandCollapserWithPriority extends HystrixCollapser<List<String>, String, PriorityRequest> {

    private final PriorityRequest request;

    public CommandCollapserWithPriority(PriorityRequest request) {
        this.request = request;
    }

    @Override
    public PriorityRequest getRequestArgument() {
        return request;
    }

    @Override
    protected HystrixCommand<List<String>> createCommand(final Collection<CollapsedRequest<String, PriorityRequest>> requests) {
        return new BatchCommand(requests);
    }

    @Override
    protected void mapResponseToRequests(List<String> batchResponse, Collection<CollapsedRequest<String, PriorityRequest>> requests) {
        int count = 0;
        for (CollapsedRequest<String, PriorityRequest> request : requests) {
            request.setResponse(batchResponse.get(count++));
        }
    }

    /**
     * Override to batch requests by priority level.
     * This method groups requests into HIGH, MEDIUM, and LOW priority batches.
     */
    @Override
    protected Collection<Collection<CollapsedRequest<String, PriorityRequest>>> batchRequestsByPriority(Collection<CollapsedRequest<String, PriorityRequest>> requests) {
        Map<Priority, List<CollapsedRequest<String, PriorityRequest>>> priorityMap = new HashMap<Priority, List<CollapsedRequest<String, PriorityRequest>>>();

        // Initialize priority buckets
        priorityMap.put(Priority.HIGH, new ArrayList<CollapsedRequest<String, PriorityRequest>>());
        priorityMap.put(Priority.MEDIUM, new ArrayList<CollapsedRequest<String, PriorityRequest>>());
        priorityMap.put(Priority.LOW, new ArrayList<CollapsedRequest<String, PriorityRequest>>());

        // Group requests by priority
        for (CollapsedRequest<String, PriorityRequest> request : requests) {
            Priority priority = request.getArgument().getPriority();
            priorityMap.get(priority).add(request);
        }

        // Return non-empty priority batches
        List<Collection<CollapsedRequest<String, PriorityRequest>>> batches = new ArrayList<Collection<CollapsedRequest<String, PriorityRequest>>>();

        // Process in priority order: HIGH, MEDIUM, LOW
        if (!priorityMap.get(Priority.HIGH).isEmpty()) {
            batches.add(priorityMap.get(Priority.HIGH));
        }
        if (!priorityMap.get(Priority.MEDIUM).isEmpty()) {
            batches.add(priorityMap.get(Priority.MEDIUM));
        }
        if (!priorityMap.get(Priority.LOW).isEmpty()) {
            batches.add(priorityMap.get(Priority.LOW));
        }

        return batches;
    }

    private static final class BatchCommand extends HystrixCommand<List<String>> {
        private final Collection<CollapsedRequest<String, PriorityRequest>> requests;

        private BatchCommand(Collection<CollapsedRequest<String, PriorityRequest>> requests) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey("GetValueWithPriority")));
            this.requests = requests;
        }

        @Override
        protected List<String> run() {
            ArrayList<String> response = new ArrayList<String>();
            for (CollapsedRequest<String, PriorityRequest> request : requests) {
                PriorityRequest pr = request.getArgument();
                response.add("Value: " + pr.getValue() + " [Priority: " + pr.getPriority() + "]");
            }
            return response;
        }
    }

    public enum Priority {
        HIGH, MEDIUM, LOW
    }

    public static class PriorityRequest {
        private final String value;
        private final Priority priority;

        public PriorityRequest(String value, Priority priority) {
            this.value = value;
            this.priority = priority;
        }

        public String getValue() {
            return value;
        }

        public Priority getPriority() {
            return priority;
        }
    }
}
