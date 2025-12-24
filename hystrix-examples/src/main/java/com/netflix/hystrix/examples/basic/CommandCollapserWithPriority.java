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

import com.netflix.hystrix.HystrixCollapser;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixRequestPriority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Example of a HystrixCollapser that uses automatic priority-based sharding.
 * Requests with different priorities will be automatically batched into separate shards.
 */
public class CommandCollapserWithPriority extends HystrixCollapser<List<String>, String, CommandCollapserWithPriority.PriorityRequest> {

    private final PriorityRequest request;

    public CommandCollapserWithPriority(int key, int priority) {
        this.request = new PriorityRequest(key, priority);
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
        for (CollapsedRequest<String, PriorityRequest> collapsedReq : requests) {
            collapsedReq.setResponse(batchResponse.get(count++));
        }
    }

    /**
     * Request object that implements HystrixRequestPriority to enable automatic priority-based sharding
     */
    public static class PriorityRequest implements HystrixRequestPriority {
        private final int key;
        private final int priority;

        public PriorityRequest(int key, int priority) {
            this.key = key;
            this.priority = priority;
        }

        public int getKey() {
            return key;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PriorityRequest that = (PriorityRequest) o;
            return key == that.key && priority == that.priority;
        }

        @Override
        public int hashCode() {
            return 31 * key + priority;
        }
    }

    private static final class BatchCommand extends HystrixCommand<List<String>> {
        private final Collection<CollapsedRequest<String, PriorityRequest>> requests;

        private BatchCommand(Collection<CollapsedRequest<String, PriorityRequest>> requests) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey("GetValueForKey")));
            this.requests = requests;
        }

        @Override
        protected List<String> run() {
            ArrayList<String> response = new ArrayList<String>();
            for (CollapsedRequest<String, PriorityRequest> request : requests) {
                // simulate fetching value for each key
                response.add("ValueForKey: " + request.getArgument().getKey() + 
                           " (Priority: " + request.getArgument().getPriority() + ")");
            }
            return response;
        }
    }
}
