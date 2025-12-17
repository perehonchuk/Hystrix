package com.netflix.hystrix;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.subjects.ReplaySubject;

public class HystrixCachedObservable<R> {
    protected final Subscription originalSubscription;
    protected final Observable<R> cachedObservable;
    private volatile int outstandingSubscriptions = 0;
    private final long creationTime;
    private final long ttlMillis;

    protected HystrixCachedObservable(final Observable<R> originalObservable) {
        this(originalObservable, -1);
    }

    protected HystrixCachedObservable(final Observable<R> originalObservable, long ttlMillis) {
        this.creationTime = System.currentTimeMillis();
        this.ttlMillis = ttlMillis;
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

    public static <R> HystrixCachedObservable<R> from(Observable<R> o, long ttlMillis) {
        return new HystrixCachedObservable<R>(o, ttlMillis);
    }

    public static <R> HystrixCachedObservable<R> from(Observable<R> o, AbstractCommand<R> originalCommand, long ttlMillis) {
        return new HystrixCommandResponseFromCache<R>(o, originalCommand, ttlMillis);
    }

    public Observable<R> toObservable() {
        return cachedObservable;
    }

    public boolean isExpired() {
        if (ttlMillis < 0) {
            return false; // no TTL set, never expires
        }
        return (System.currentTimeMillis() - creationTime) > ttlMillis;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getTtlMillis() {
        return ttlMillis;
    }

    public void unsubscribe() {
        originalSubscription.unsubscribe();
    }
}
