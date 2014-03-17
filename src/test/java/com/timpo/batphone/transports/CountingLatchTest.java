package com.timpo.batphone.transports;

import com.timpo.batphone.transports.polling.CountingLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

public class CountingLatchTest {

    private CountingLatch instance;

    @Test
    public void testCountUp() {
        instance = new CountingLatch();
        instance.countUp();
        try {
            instance.await(1, TimeUnit.SECONDS);
            fail();
        } catch (InterruptedException ex) {
        }
    }

    @Test
    public void testCountDown() {
        instance = new CountingLatch(1);
        instance.countDown();
        try {
            instance.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            fail();
        }
    }

    @Test
    public void testCountUpCountDown() {
        instance = new CountingLatch(0);
        instance.countUp();
        instance.countDown();
        try {
            instance.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            fail();
        }
    }

    @Test
    public void testNoCount() {
        instance = new CountingLatch();
        try {
            instance.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            fail();
        }
    }
}