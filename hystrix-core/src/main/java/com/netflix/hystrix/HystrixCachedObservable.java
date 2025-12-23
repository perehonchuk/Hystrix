package com.netflix.hystrix;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.subjects.ReplaySubject;

public class HystrixCachedObservable<R> {
    protected final Subscription originalSubscription;
    protected final Observable<R> cachedObservable;
    private volatile int outstandingSubscriptions = 0;
    private final long cachedAtTimestamp;
    private volatile boolean markedAsStale = false;

    protected HystrixCachedObservable(final Observable<R> originalObservable) {
        this.cachedAtTimestamp = System.currentTimeMillis();
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
     * Get the timestamp when this observable was cached
     * @return timestamp in milliseconds
     */
    public long getCachedAtTimestamp() {
        return cachedAtTimestamp;
    }

    /**
     * Check if the cached value is stale based on the provided TTL
     * @param staleAfterMilliseconds the TTL in milliseconds
     * @return true if the cached value is older than the TTL
     */
    public boolean isStale(long staleAfterMilliseconds) {
        return (System.currentTimeMillis() - cachedAtTimestamp) > staleAfterMilliseconds;
    }

    /**
     * Mark this cached observable as stale to trigger background refresh
     */
    public void markAsStale() {
        this.markedAsStale = true;
    }

    /**
     * Check if this observable has been marked as stale
     * @return true if marked as stale
     */
    public boolean isMarkedAsStale() {
        return markedAsStale;
    }
}
