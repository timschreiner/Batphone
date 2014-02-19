package com.timpo.batphone.transports;

import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

public class CountingLatchTest {

    private CountingLatch instance;

    @Before
    public void setUp() {
        instance = new CountingLatch();
    }

    @Test
    public void testCountUp() {
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
            fail();
        } catch (InterruptedException ex) {
        }
    }

    @Test
    public void testCountUpCountDown() {
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
        try {
            instance.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            fail();
        }
    }
}