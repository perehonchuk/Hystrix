package com.netflix.hystrix;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.subjects.ReplaySubject;

public class HystrixCachedObservable<R> {
    protected final Subscription originalSubscription;
    protected final Observable<R> cachedObservable;
    private volatile int outstandingSubscriptions = 0;
    private final long creationTimestamp;
    private final long ttlInMillis;

    protected HystrixCachedObservable(final Observable<R> originalObservable) {
        this(originalObservable, -1);
    }

    protected HystrixCachedObservable(final Observable<R> originalObservable, long ttlInMillis) {
        this.creationTimestamp = System.currentTimeMillis();
        this.ttlInMillis = ttlInMillis;
        ReplaySubject<R> replaySubject = ReplaySubject.create();
        this.originalSubscription = originalObservable
                .subscribe(replaySubject);

        this.cachedObservable = replaySubject
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        outstandingSubscriptions--;
                        if (outstandingSubscriptions == 0) {
                            originalSubscription.unsubscribe();
                        }
                    }
                })
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        outstandingSubscriptions++;
                    }
                });
    }

    public static <R> HystrixCachedObservable<R> from(Observable<R> o, AbstractCommand<R> originalCommand) {
        return new HystrixCommandResponseFromCache<R>(o, originalCommand);
    }

    public static <R> HystrixCachedObservable<R> from(Observable<R> o) {
        return new HystrixCachedObservable<R>(o);
    }

    public Observable<R> toObservable() {
        return cachedObservable;
    }

    public void unsubscribe() {
        originalSubscription.unsubscribe();
    }

    /**
     * Check if this cached observable has expired based on TTL.
     * @return true if TTL is enabled and the cache entry has expired, false otherwise
     */
    public boolean isExpired() {
        if (ttlInMillis <= 0) {
            return false; // TTL not enabled
        }
        long age = System.currentTimeMillis() - creationTimestamp;
        return age > ttlInMillis;
    }

    /**
     * Get the creation timestamp of this cached observable.
     * @return timestamp in milliseconds when this cache entry was created
     */
    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    /**
     * Get the TTL in milliseconds for this cached observable.
     * @return TTL in milliseconds, or -1 if TTL is not enabled
     */
    public long getTtlInMillis() {
        return ttlInMillis;
    }
}
