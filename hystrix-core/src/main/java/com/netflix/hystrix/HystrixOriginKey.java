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

import com.netflix.hystrix.util.InternMap;

/**
 * A key to represent the origin/source of a {@link HystrixCommand} for monitoring,
 * metrics publishing, and observability purposes. The origin typically represents
 * the service, subsystem, or team that owns the command.
 * <p>
 * This interface is intended to work natively with Enums so that implementing code
 * can be an enum that implements this interface.
 */
public interface HystrixOriginKey extends HystrixKey {
    class Factory {
        private Factory() {
        }

        // used to intern instances so we don't keep re-creating them millions of times for the same key
        private static final InternMap<String, HystrixOriginKeyDefault> intern
                = new InternMap<String, HystrixOriginKeyDefault>(
                new InternMap.ValueConstructor<String, HystrixOriginKeyDefault>() {
                    @Override
                    public HystrixOriginKeyDefault create(String key) {
                        return new HystrixOriginKeyDefault(key);
                    }
                });


        /**
         * Retrieve (or create) an interned HystrixOriginKey instance for a given name.
         *
         * @param name origin name (e.g., "user-service", "payment-api", "recommendation-engine")
         * @return HystrixOriginKey instance that is interned (cached) so a given name will always retrieve the same instance.
         */
        public static HystrixOriginKey asKey(String name) {
            return intern.interned(name);
        }

        private static class HystrixOriginKeyDefault extends HystrixKey.HystrixKeyDefault implements HystrixOriginKey {
            public HystrixOriginKeyDefault(String name) {
                super(name);
            }
        }

        /* package-private */ static int getOriginCount() {
            return intern.size();
        }
    }

}
