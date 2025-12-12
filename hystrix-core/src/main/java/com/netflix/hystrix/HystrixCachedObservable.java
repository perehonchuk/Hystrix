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
    private volatile long lastAccessTimestamp;

    protected HystrixCachedObservable(final Observable<R> originalObservable) {
        this.creationTimestamp = System.currentTimeMillis();
        this.lastAccessTimestamp = this.creationTimestamp;
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
                        lastAccessTimestamp = System.currentTimeMillis();
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

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public long getLastAccessTimestamp() {
        return lastAccessTimestamp;
    }

    public long getAgeInMilliseconds() {
        return System.currentTimeMillis() - creationTimestamp;
    }

    public long getIdleTimeInMilliseconds() {
        return System.currentTimeMillis() - lastAccessTimestamp;
    }

    public boolean isExpired(long maxAgeMillis) {
        return maxAgeMillis > 0 && getAgeInMilliseconds() > maxAgeMillis;
    }

    public boolean isIdle(long maxIdleMillis) {
        return maxIdleMillis > 0 && getIdleTimeInMilliseconds() > maxIdleMillis;
    }
}
