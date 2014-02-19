package com.timpo.batphone.metrics;

import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class InstrumentedFuture<T> implements ListenableFuture<T> {

    private final ListenableFuture<T> wrappedFuture;
    private final Timer.Context completionTimerContext;

    public InstrumentedFuture(ListenableFuture<T> wrappedFuture, Timer.Context completionTimerContext) {
        this.wrappedFuture = wrappedFuture;
        this.completionTimerContext = completionTimerContext;
    }

    @Override
    public void addListener(final Runnable listener, Executor executor) {
        wrappedFuture.addListener(new Runnable() {
            @Override
            public void run() {
                completionTimerContext.stop();
                listener.run();
            }
        }, executor);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        completionTimerContext.stop();

        return wrappedFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return wrappedFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return wrappedFuture.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        try {
            return wrappedFuture.get();

        } finally {
            completionTimerContext.stop();
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return wrappedFuture.get(timeout, unit);

        } finally {
            completionTimerContext.stop();
        }
    }
}
