package com.timpo.batphone.transports;

import com.timpo.batphone.other.Utils;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CountingLatch {

    private final AtomicInteger counter;

    public CountingLatch(int count) {
        counter = new AtomicInteger(count);
    }

    public CountingLatch() {
        counter = new AtomicInteger(0);
    }

    public void countUp() {
        counter.incrementAndGet();
    }

    public void countDown() {
        counter.decrementAndGet();
    }

    @SuppressWarnings("SleepWhileInLoop")
    public void await() throws InterruptedException {
        while (counter.get() > 0) {
            Thread.sleep(1);
        }
    }

    @SuppressWarnings("SleepWhileInLoop")
    public void await(long duration, TimeUnit timeUnit) throws InterruptedException {
        long endTime = System.currentTimeMillis() + Utils.toMillis(duration, timeUnit);

        while (counter.get() > 0) {
            if (System.currentTimeMillis() > endTime) {
                throw new InterruptedException("timed out while awaiting the latch to reach 0");
            }

            Thread.sleep(1);
        }
    }
}
