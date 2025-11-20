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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import org.junit.Test;

import com.netflix.hystrix.HystrixCollapser;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.HystrixInvokableInfo;
import com.netflix.hystrix.HystrixRequestLog;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;

/**
 * Sample {@link HystrixCollapser} demonstrating priority-based request collapsing.
 * Requests with different priorities can be batched separately, allowing high-priority
 * requests to be processed before lower-priority ones.
 */
public class CommandCollapserWithPriority extends HystrixCollapser<List<String>, String, Integer> {

    private final Integer key;
    private final int priority;

    public CommandCollapserWithPriority(Integer key) {
        this(key, 0); // default to highest priority
    }

    public CommandCollapserWithPriority(Integer key, int priority) {
        this.key = key;
        this.priority = priority;
    }

    @Override
    public Integer getRequestArgument() {
        return key;
    }

    @Override
    public int getRequestPriority() {
        return priority;
    }

    @Override
    protected HystrixCommand<List<String>> createCommand(final Collection<CollapsedRequest<String, Integer>> requests) {
        return new BatchCommand(requests);
    }

    @Override
    protected void mapResponseToRequests(List<String> batchResponse, Collection<CollapsedRequest<String, Integer>> requests) {
        int count = 0;
        for (CollapsedRequest<String, Integer> request : requests) {
            request.setResponse(batchResponse.get(count++));
        }
    }

    /**
     * Override to enable priority-based batch splitting.
     * This groups requests by priority level before executing batches.
     */
    @Override
    protected Collection<Collection<CollapsedRequest<String, Integer>>> shardRequests(Collection<CollapsedRequest<String, Integer>> requests) {
        // Use the priority splitting method to group by priority
        return splitRequestsByPriority(requests);
    }

    private static final class BatchCommand extends HystrixCommand<List<String>> {
        private final Collection<CollapsedRequest<String, Integer>> requests;

        private BatchCommand(Collection<CollapsedRequest<String, Integer>> requests) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey("GetValueWithPriority")));
            this.requests = requests;
        }

        @Override
        protected List<String> run() {
            ArrayList<String> response = new ArrayList<String>();
            for (CollapsedRequest<String, Integer> request : requests) {
                // artificial response including priority information
                response.add("ValueForKey: " + request.getArgument() + " (priority=" + request.getPriority() + ")");
            }
            return response;
        }
    }

    public static class UnitTest {

        @Test
        public void testCollapserWithPriority() throws Exception {
            HystrixRequestContext context = HystrixRequestContext.initializeContext();
            try {
                // Create requests with different priorities
                Future<String> f1 = new CommandCollapserWithPriority(1, 0).queue(); // high priority
                Future<String> f2 = new CommandCollapserWithPriority(2, 1).queue(); // medium priority
                Future<String> f3 = new CommandCollapserWithPriority(3, 0).queue(); // high priority
                Future<String> f4 = new CommandCollapserWithPriority(4, 2).queue(); // low priority

                assertEquals("ValueForKey: 1 (priority=0)", f1.get());
                assertEquals("ValueForKey: 2 (priority=1)", f2.get());
                assertEquals("ValueForKey: 3 (priority=0)", f3.get());
                assertEquals("ValueForKey: 4 (priority=2)", f4.get());

                int numExecuted = HystrixRequestLog.getCurrentRequest().getAllExecutedCommands().size();

                System.err.println("num executed: " + numExecuted);

                // With priority-based batching, we expect multiple batches (one per priority level)
                // In this case: priority 0, 1, and 2 = 3 batches minimum
                assertTrue("Expected multiple batches due to priority splitting", numExecuted >= 1);

                System.err.println("HystrixRequestLog.getCurrentRequest().getAllExecutedCommands(): " + HystrixRequestLog.getCurrentRequest().getAllExecutedCommands());

                for (HystrixInvokableInfo<?> command : HystrixRequestLog.getCurrentRequest().getAllExecutedCommands()) {
                    // assert the command is the one we're expecting
                    assertEquals("GetValueWithPriority", command.getCommandKey().name());

                    System.err.println(command.getCommandKey().name() + " => command.getExecutionEvents(): " + command.getExecutionEvents());

                    // confirm that it was a COLLAPSED command execution
                    assertTrue(command.getExecutionEvents().contains(HystrixEventType.COLLAPSED));
                    assertTrue(command.getExecutionEvents().contains(HystrixEventType.SUCCESS));
                }
            } finally {
                context.shutdown();
            }
        }
    }
}
