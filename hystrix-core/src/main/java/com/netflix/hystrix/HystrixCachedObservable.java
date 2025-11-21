package com.netflix.hystrix;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.subjects.ReplaySubject;

public class HystrixCachedObservable<R> {
    protected final Subscription originalSubscription;
    protected final Observable<R> cachedObservable;
    private volatile int outstandingSubscriptions = 0;
    private final long createdTimestamp;

    protected HystrixCachedObservable(final Observable<R> originalObservable) {
        this.createdTimestamp = System.currentTimeMillis();
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
     * Returns the timestamp when this cached observable was created.
     * Used for TTL-based cache expiration.
     *
     * @return creation timestamp in milliseconds since epoch
     */
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    /**
     * Checks if this cached entry has expired based on the given TTL.
     *
     * @param ttlInMilliseconds the time-to-live in milliseconds (0 or negative means never expires)
     * @return true if the entry has expired, false otherwise
     */
    public boolean isExpired(int ttlInMilliseconds) {
        if (ttlInMilliseconds <= 0) {
            return false;
        }
        return (System.currentTimeMillis() - createdTimestamp) > ttlInMilliseconds;
    }
}
