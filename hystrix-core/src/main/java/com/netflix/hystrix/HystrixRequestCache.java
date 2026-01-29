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

import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariableDefault;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariableHolder;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariableLifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.internal.operators.CachedObservable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * Cache that is scoped to the current request as managed by {@link HystrixRequestVariableDefault}.
 * <p>
 * This is used for short-lived caching of {@link HystrixCommand} instances to allow de-duping of command executions within a request.
 */
public class HystrixRequestCache {
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(HystrixRequestCache.class);

    // the String key must be: HystrixRequestCache.prefix + concurrencyStrategy + cacheKey
    private final static ConcurrentHashMap<RequestCacheKey, HystrixRequestCache> caches = new ConcurrentHashMap<RequestCacheKey, HystrixRequestCache>();

    // Global registry of cache invalidation listeners
    private final static ConcurrentHashMap<RequestCacheKey, CopyOnWriteArrayList<HystrixCacheInvalidationListener>> invalidationListeners = new ConcurrentHashMap<RequestCacheKey, CopyOnWriteArrayList<HystrixCacheInvalidationListener>>();

    private final RequestCacheKey rcKey;
    private final HystrixConcurrencyStrategy concurrencyStrategy;

    /**
     * A ConcurrentHashMap per 'prefix' and per request scope that is used to to dedupe requests in the same request.
     * <p>
     * Key => CommandPrefix + CacheKey : Future<?> from queue()
     */
    private static final HystrixRequestVariableHolder<ConcurrentHashMap<ValueCacheKey, HystrixCachedObservable<?>>> requestVariableForCache = new HystrixRequestVariableHolder<ConcurrentHashMap<ValueCacheKey, HystrixCachedObservable<?>>>(new HystrixRequestVariableLifecycle<ConcurrentHashMap<ValueCacheKey, HystrixCachedObservable<?>>>() {

        @Override
        public ConcurrentHashMap<ValueCacheKey, HystrixCachedObservable<?>> initialValue() {
            return new ConcurrentHashMap<ValueCacheKey, HystrixCachedObservable<?>>();
        }

        @Override
        public void shutdown(ConcurrentHashMap<ValueCacheKey, HystrixCachedObservable<?>> value) {
            // nothing to shutdown
        }

    });

    private HystrixRequestCache(RequestCacheKey rcKey, HystrixConcurrencyStrategy concurrencyStrategy) {
        this.rcKey = rcKey;
        this.concurrencyStrategy = concurrencyStrategy;
    }

    public static HystrixRequestCache getInstance(HystrixCommandKey key, HystrixConcurrencyStrategy concurrencyStrategy) {
        return getInstance(new RequestCacheKey(key, concurrencyStrategy), concurrencyStrategy);
    }

    public static HystrixRequestCache getInstance(HystrixCollapserKey key, HystrixConcurrencyStrategy concurrencyStrategy) {
        return getInstance(new RequestCacheKey(key, concurrencyStrategy), concurrencyStrategy);
    }

    private static HystrixRequestCache getInstance(RequestCacheKey rcKey, HystrixConcurrencyStrategy concurrencyStrategy) {
        HystrixRequestCache c = caches.get(rcKey);
        if (c == null) {
            HystrixRequestCache newRequestCache = new HystrixRequestCache(rcKey, concurrencyStrategy);
            HystrixRequestCache existing = caches.putIfAbsent(rcKey, newRequestCache);
            if (existing == null) {
                // we won so use the new one
                c = newRequestCache;
            } else {
                // we lost so use the existing
                c = existing;
            }
        }
        return c;
    }

    /**
     * Retrieve a cached Future for this request scope if a matching command has already been executed/queued.
     * 
     * @return {@code Future<T>}
     */
    // suppressing warnings because we are using a raw Future since it's in a heterogeneous ConcurrentHashMap cache
    @SuppressWarnings({ "unchecked" })
    /* package */<T> HystrixCachedObservable<T> get(String cacheKey) {
        ValueCacheKey key = getRequestCacheKey(cacheKey);
        if (key != null) {
            ConcurrentHashMap<ValueCacheKey, HystrixCachedObservable<?>> cacheInstance = requestVariableForCache.get(concurrencyStrategy);
            if (cacheInstance == null) {
                throw new IllegalStateException("Request caching is not available. Maybe you need to initialize the HystrixRequestContext?");
            }
            /* look for the stored value */
            return (HystrixCachedObservable<T>) cacheInstance.get(key);
        }
        return null;
    }

    /**
     * Put the Future in the cache if it does not already exist.
     * <p>
     * If this method returns a non-null value then another thread won the race and it should be returned instead of proceeding with execution of the new Future.
     * 
     * @param cacheKey
     *            key as defined by {@link HystrixCommand#getCacheKey()}
     * @param f
     *            Future to be cached
     * 
     * @return null if nothing else was in the cache (or this {@link HystrixCommand} does not have a cacheKey) or previous value if another thread beat us to adding to the cache
     */
    // suppressing warnings because we are using a raw Future since it's in a heterogeneous ConcurrentHashMap cache
    @SuppressWarnings({ "unchecked" })
    /* package */<T> HystrixCachedObservable<T> putIfAbsent(String cacheKey, HystrixCachedObservable<T> f) {
        ValueCacheKey key = getRequestCacheKey(cacheKey);
        if (key != null) {
            /* look for the stored value */
            ConcurrentHashMap<ValueCacheKey, HystrixCachedObservable<?>> cacheInstance = requestVariableForCache.get(concurrencyStrategy);
            if (cacheInstance == null) {
                throw new IllegalStateException("Request caching is not available. Maybe you need to initialize the HystrixRequestContext?");
            }
            HystrixCachedObservable<T> alreadySet = (HystrixCachedObservable<T>) cacheInstance.putIfAbsent(key, f);
            if (alreadySet != null) {
                // someone beat us so we didn't cache this
                return alreadySet;
            }
        }
        // we either set it in the cache or do not have a cache key
        return null;
    }

    /**
     * Clear the cache for a given cacheKey.
     *
     * @param cacheKey
     *            key as defined by {@link HystrixCommand#getCacheKey()}
     */
    public void clear(String cacheKey) {
        clear(cacheKey, "explicit");
    }

    /**
     * Clear the cache for a given cacheKey with a specified reason.
     * <p>
     * This method will notify all registered invalidation listeners after removing the cache entry.
     *
     * @param cacheKey
     *            key as defined by {@link HystrixCommand#getCacheKey()}
     * @param reason
     *            the reason for cache invalidation (e.g., "explicit", "timeout", "pattern")
     */
    public void clear(String cacheKey, String reason) {
        ValueCacheKey key = getRequestCacheKey(cacheKey);
        if (key != null) {
            ConcurrentHashMap<ValueCacheKey, HystrixCachedObservable<?>> cacheInstance = requestVariableForCache.get(concurrencyStrategy);
            if (cacheInstance == null) {
                throw new IllegalStateException("Request caching is not available. Maybe you need to initialize the HystrixRequestContext?");
            }

            /* remove this cache key */
            HystrixCachedObservable<?> removed = cacheInstance.remove(key);

            /* notify listeners if entry was actually removed */
            if (removed != null) {
                notifyInvalidationListeners(cacheKey, reason);
            }
        }
    }

    /**
     * Clear all cache entries matching a pattern.
     * <p>
     * This allows wildcard-based cache invalidation using regular expressions.
     * For example, "user:.*" would invalidate all cache keys starting with "user:".
     *
     * @param pattern
     *            regular expression pattern to match cache keys
     * @return number of cache entries cleared
     */
    public int clearByPattern(String pattern) {
        Pattern regex = Pattern.compile(pattern);
        int clearedCount = 0;

        ConcurrentHashMap<ValueCacheKey, HystrixCachedObservable<?>> cacheInstance = requestVariableForCache.get(concurrencyStrategy);
        if (cacheInstance == null) {
            throw new IllegalStateException("Request caching is not available. Maybe you need to initialize the HystrixRequestContext?");
        }

        List<String> keysToRemove = new ArrayList<String>();
        for (Map.Entry<ValueCacheKey, HystrixCachedObservable<?>> entry : cacheInstance.entrySet()) {
            String valueCacheKey = entry.getKey().valueCacheKey;
            if (regex.matcher(valueCacheKey).matches()) {
                keysToRemove.add(valueCacheKey);
            }
        }

        for (String keyToRemove : keysToRemove) {
            clear(keyToRemove, "pattern:" + pattern);
            clearedCount++;
        }

        return clearedCount;
    }

    /**
     * Register a cache invalidation listener for this cache.
     * <p>
     * Listeners will be notified whenever a cache entry is cleared.
     *
     * @param listener
     *            the listener to register
     */
    public void registerInvalidationListener(HystrixCacheInvalidationListener listener) {
        CopyOnWriteArrayList<HystrixCacheInvalidationListener> listeners = invalidationListeners.get(rcKey);
        if (listeners == null) {
            CopyOnWriteArrayList<HystrixCacheInvalidationListener> newListeners = new CopyOnWriteArrayList<HystrixCacheInvalidationListener>();
            CopyOnWriteArrayList<HystrixCacheInvalidationListener> existing = invalidationListeners.putIfAbsent(rcKey, newListeners);
            listeners = (existing != null) ? existing : newListeners;
        }
        listeners.add(listener);
    }

    /**
     * Unregister a cache invalidation listener.
     *
     * @param listener
     *            the listener to unregister
     * @return true if the listener was registered and has been removed
     */
    public boolean unregisterInvalidationListener(HystrixCacheInvalidationListener listener) {
        CopyOnWriteArrayList<HystrixCacheInvalidationListener> listeners = invalidationListeners.get(rcKey);
        if (listeners != null) {
            return listeners.remove(listener);
        }
        return false;
    }

    /**
     * Notify all registered listeners that a cache entry has been invalidated.
     */
    private void notifyInvalidationListeners(String cacheKey, String reason) {
        CopyOnWriteArrayList<HystrixCacheInvalidationListener> listeners = invalidationListeners.get(rcKey);
        if (listeners != null && !listeners.isEmpty()) {
            for (HystrixCacheInvalidationListener listener : listeners) {
                try {
                    if (rcKey.type == 1) {
                        // Command key
                        listener.onCacheInvalidated(HystrixCommandKey.Factory.asKey(rcKey.key), cacheKey, reason);
                    } else if (rcKey.type == 2) {
                        // Collapser key
                        listener.onCacheInvalidated(HystrixCollapserKey.Factory.asKey(rcKey.key), cacheKey, reason);
                    }
                } catch (Exception e) {
                    logger.error("Error notifying cache invalidation listener", e);
                }
            }
        }
    }

    /**
     * Request CacheKey: HystrixRequestCache.prefix + concurrencyStrategy + HystrixCommand.getCacheKey (as injected via get/put to this class)
     * <p>
     * We prefix with {@link HystrixCommandKey} or {@link HystrixCollapserKey} since the cache is heterogeneous and we don't want to accidentally return cached Futures from different
     * types.
     * 
     * @return ValueCacheKey
     */
    private ValueCacheKey getRequestCacheKey(String cacheKey) {
        if (cacheKey != null) {
            /* create the cache key we will use to retrieve/store that include the type key prefix */
            return new ValueCacheKey(rcKey, cacheKey);
        }
        return null;
    }

    private static class ValueCacheKey {
        private final RequestCacheKey rvKey;
        private final String valueCacheKey;

        private ValueCacheKey(RequestCacheKey rvKey, String valueCacheKey) {
            this.rvKey = rvKey;
            this.valueCacheKey = valueCacheKey;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((rvKey == null) ? 0 : rvKey.hashCode());
            result = prime * result + ((valueCacheKey == null) ? 0 : valueCacheKey.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ValueCacheKey other = (ValueCacheKey) obj;
            if (rvKey == null) {
                if (other.rvKey != null)
                    return false;
            } else if (!rvKey.equals(other.rvKey))
                return false;
            if (valueCacheKey == null) {
                if (other.valueCacheKey != null)
                    return false;
            } else if (!valueCacheKey.equals(other.valueCacheKey))
                return false;
            return true;
        }

    }

    private static class RequestCacheKey {
        private final short type; // used to differentiate between Collapser/Command if key is same between them
        private final String key;
        private final HystrixConcurrencyStrategy concurrencyStrategy;

        private RequestCacheKey(HystrixCommandKey commandKey, HystrixConcurrencyStrategy concurrencyStrategy) {
            type = 1;
            if (commandKey == null) {
                this.key = null;
            } else {
                this.key = commandKey.name();
            }
            this.concurrencyStrategy = concurrencyStrategy;
        }

        private RequestCacheKey(HystrixCollapserKey collapserKey, HystrixConcurrencyStrategy concurrencyStrategy) {
            type = 2;
            if (collapserKey == null) {
                this.key = null;
            } else {
                this.key = collapserKey.name();
            }
            this.concurrencyStrategy = concurrencyStrategy;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((concurrencyStrategy == null) ? 0 : concurrencyStrategy.hashCode());
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + type;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            RequestCacheKey other = (RequestCacheKey) obj;
            if (type != other.type)
                return false;
            if (key == null) {
                if (other.key != null)
                    return false;
            } else if (!key.equals(other.key))
                return false;
            if (concurrencyStrategy == null) {
                if (other.concurrencyStrategy != null)
                    return false;
            } else if (!concurrencyStrategy.equals(other.concurrencyStrategy))
                return false;
            return true;
        }

    }

}
