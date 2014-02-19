package com.timpo.batphone.other;


import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.fail;

public class TestUtils {

    public static void failUnlessConditionMetWithin(AtomicBoolean condition, long duration, TimeUnit timeUnit) {
        long startTime = System.currentTimeMillis();
        long waitUntil = startTime + Utils.toMillis(duration, timeUnit);
        while (System.currentTimeMillis() <= waitUntil) {
            if (condition.get()) {
                return;
            }
            Utils.sleep(10, TimeUnit.MILLISECONDS);
        }

        fail();
    }
}
