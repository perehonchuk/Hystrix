package com.netflix.hystrix;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.subjects.ReplaySubject;

public class HystrixCachedObservable<R> {
    protected final Subscription originalSubscription;
    protected final Observable<R> cachedObservable;
    private volatile int outstandingSubscriptions = 0;
    protected final long creationTimestamp;
    protected final long ttlInMilliseconds;

    protected HystrixCachedObservable(final Observable<R> originalObservable) {
        this(originalObservable, -1);
    }

    protected HystrixCachedObservable(final Observable<R> originalObservable, long ttlInMilliseconds) {
        this.creationTimestamp = System.currentTimeMillis();
        this.ttlInMilliseconds = ttlInMilliseconds;
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

    public static <R> HystrixCachedObservable<R> from(Observable<R> o, long ttlInMilliseconds) {
        return new HystrixCachedObservable<R>(o, ttlInMilliseconds);
    }

    public static <R> HystrixCachedObservable<R> from(Observable<R> o, AbstractCommand<R> originalCommand, long ttlInMilliseconds) {
        return new HystrixCommandResponseFromCache<R>(o, originalCommand, ttlInMilliseconds);
    }

    public Observable<R> toObservable() {
        return cachedObservable;
    }

    public boolean isExpired() {
        if (ttlInMilliseconds < 0) {
            return false; // No TTL configured, never expires
        }
        long currentTime = System.currentTimeMillis();
        return (currentTime - creationTimestamp) > ttlInMilliseconds;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public long getTtlInMilliseconds() {
        return ttlInMilliseconds;
    }

    public void unsubscribe() {
        originalSubscription.unsubscribe();
    }
}
