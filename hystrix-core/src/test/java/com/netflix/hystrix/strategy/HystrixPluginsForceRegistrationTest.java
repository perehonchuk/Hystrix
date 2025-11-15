/**
 * Copyright 2016 Netflix, Inc.
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
package com.netflix.hystrix.strategy;

import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifierDefault;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategyDefault;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherDefault;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategyDefault;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHookDefault;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test that validates the force registration methods allow plugin re-registration
 * without throwing IllegalStateException.
 */
public class HystrixPluginsForceRegistrationTest {

    @After
    public void cleanup() {
        HystrixPlugins.reset();
    }

    @Test
    public void testForceRegisterEventNotifierAllowsReregistration() {
        HystrixEventNotifier first = HystrixEventNotifierDefault.getInstance();
        HystrixEventNotifier second = HystrixEventNotifierDefault.getInstance();

        HystrixPlugins.getInstance().forceRegisterEventNotifier(first);
        assertEquals(first, HystrixPlugins.getInstance().getEventNotifier());

        // This should not throw IllegalStateException
        HystrixPlugins.getInstance().forceRegisterEventNotifier(second);
        assertEquals(second, HystrixPlugins.getInstance().getEventNotifier());
    }

    @Test
    public void testForceRegisterConcurrencyStrategyAllowsReregistration() {
        HystrixConcurrencyStrategy first = HystrixConcurrencyStrategyDefault.getInstance();
        HystrixConcurrencyStrategy second = HystrixConcurrencyStrategyDefault.getInstance();

        HystrixPlugins.getInstance().forceRegisterConcurrencyStrategy(first);
        assertEquals(first, HystrixPlugins.getInstance().getConcurrencyStrategy());

        // This should not throw IllegalStateException
        HystrixPlugins.getInstance().forceRegisterConcurrencyStrategy(second);
        assertEquals(second, HystrixPlugins.getInstance().getConcurrencyStrategy());
    }

    @Test
    public void testForceRegisterMetricsPublisherAllowsReregistration() {
        HystrixMetricsPublisher first = HystrixMetricsPublisherDefault.getInstance();
        HystrixMetricsPublisher second = HystrixMetricsPublisherDefault.getInstance();

        HystrixPlugins.getInstance().forceRegisterMetricsPublisher(first);
        assertEquals(first, HystrixPlugins.getInstance().getMetricsPublisher());

        // This should not throw IllegalStateException
        HystrixPlugins.getInstance().forceRegisterMetricsPublisher(second);
        assertEquals(second, HystrixPlugins.getInstance().getMetricsPublisher());
    }

    @Test
    public void testForceRegisterPropertiesStrategyAllowsReregistration() {
        HystrixPropertiesStrategy first = HystrixPropertiesStrategyDefault.getInstance();
        HystrixPropertiesStrategy second = HystrixPropertiesStrategyDefault.getInstance();

        HystrixPlugins.getInstance().forceRegisterPropertiesStrategy(first);
        assertEquals(first, HystrixPlugins.getInstance().getPropertiesStrategy());

        // This should not throw IllegalStateException
        HystrixPlugins.getInstance().forceRegisterPropertiesStrategy(second);
        assertEquals(second, HystrixPlugins.getInstance().getPropertiesStrategy());
    }

    @Test
    public void testForceRegisterCommandExecutionHookAllowsReregistration() {
        HystrixCommandExecutionHook first = HystrixCommandExecutionHookDefault.getInstance();
        HystrixCommandExecutionHook second = HystrixCommandExecutionHookDefault.getInstance();

        HystrixPlugins.getInstance().forceRegisterCommandExecutionHook(first);
        assertEquals(first, HystrixPlugins.getInstance().getCommandExecutionHook());

        // This should not throw IllegalStateException
        HystrixPlugins.getInstance().forceRegisterCommandExecutionHook(second);
        assertEquals(second, HystrixPlugins.getInstance().getCommandExecutionHook());
    }

    @Test
    public void testRegularRegisterStillThrowsOnDuplicateRegistration() {
        HystrixEventNotifier first = HystrixEventNotifierDefault.getInstance();
        HystrixEventNotifier second = HystrixEventNotifierDefault.getInstance();

        HystrixPlugins.getInstance().registerEventNotifier(first);

        try {
            HystrixPlugins.getInstance().registerEventNotifier(second);
            fail("Expected IllegalStateException to be thrown");
        } catch (IllegalStateException e) {
            assertEquals("Another strategy was already registered.", e.getMessage());
        }
    }
}
