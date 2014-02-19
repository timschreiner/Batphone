package com.timpo.batphone.other;

import static com.timpo.batphone.other.Utils.JSON;
import com.timpo.batphone.transports.BinaryMessage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

public class UtilsTest {

    @Test
    public void testSimpleID() {
        int expectedSize = 1000;

        Set<String> requestIDs = new HashSet<>();
        for (int i = 0; i < expectedSize; i++) {
            requestIDs.add(Utils.simpleID());
        }

        assertEquals(expectedSize, requestIDs.size());
    }

    @Test
    public void testUniqueID() {
        int expectedSize = 1000;

        Set<String> messageIDs = new HashSet<>();
        for (int i = 0; i < expectedSize; i++) {
            messageIDs.add(Utils.uniqueID());
        }

        assertEquals(expectedSize, messageIDs.size());
    }

    @Test
    public void testAsBytesAndAsString() {
        String expected = "hello";
        byte[] bytes = Utils.asBytes(expected);
        String actual = Utils.asString(bytes);

        assertEquals(expected, actual);
    }

    @Test
    public void testConvertToMap() {
        String expectedKey = "key";
        byte[] expectedPayload = Utils.asBytes("payload");

        BinaryMessage bm = new BinaryMessage(expectedKey, expectedPayload);
        Map<String, Object> r = Utils.convertToMap(bm);

        assertEquals(expectedKey, r.get("key"));
        assertArrayEquals(expectedPayload, (byte[]) r.get("payload"));
    }

    @Test
    public void testIsWildcard() {
        String channel = "simple.channel";
        boolean expResult = false;
        boolean result = Utils.isWildcard(channel);
        assertEquals(expResult, result);

        channel = "wildcard.*";
        expResult = true;
        result = Utils.isWildcard(channel);
        assertEquals(expResult, result);

        channel = "wildcard.thing-1";
        expResult = false;
        result = Utils.isWildcard(channel);
        assertEquals(expResult, result);
    }

    @Test
    public void testSleep() throws InterruptedException {

        long duration = 1L;
        TimeUnit timeUnit = TimeUnit.SECONDS;

        int expectedDurationMs = 1000;
        int acceptableDriftMs = 1; //drift should be under 1 milliseconds

        //this call just warms up the sleep method, otherwise the drift is huge
        Utils.sleep(duration, timeUnit);

        long timeStart = System.nanoTime();

        Utils.sleep(duration, timeUnit);

        long timeEnd = System.nanoTime();
        long timeDiff = timeEnd - timeStart;
        int actualDurationMs = (int) timeDiff / 1000000;
        int actualDriftMs = Math.abs(expectedDurationMs - actualDurationMs);

        assertTrue(actualDriftMs <= acceptableDriftMs);
    }

    @Test
    public void testSecondsFromNowAndLoopUntil() {
        int expectedDurationMs = 1000;
        int acceptableDriftMs = 1; //drift should be under 5 milliseconds

        long endTime = Utils.secondsFromNow(1);

        long timeStart = System.nanoTime();

        while (Utils.loopUntil(endTime)) {
            Utils.sleep(500, TimeUnit.MICROSECONDS);
        }

        long timeEnd = System.nanoTime();
        long timeDiff = timeEnd - timeStart;
        int actualDurationMs = (int) timeDiff / 1000000;
        int actualDriftMs = Math.abs(expectedDurationMs - actualDurationMs);

        assertTrue(actualDriftMs <= acceptableDriftMs);
    }

    @Test
    public void testToMillis() {
        long duration = 1L;
        TimeUnit timeUnit = TimeUnit.SECONDS;
        long expResult = 1000L;
        long result = Utils.toMillis(duration, timeUnit);
        assertEquals(expResult, result);

        duration = 1L;
        timeUnit = TimeUnit.MILLISECONDS;
        expResult = 1L;
        result = Utils.toMillis(duration, timeUnit);
        assertEquals(expResult, result);

        duration = 2L;
        timeUnit = TimeUnit.MINUTES;
        expResult = 1000 * 60 * 2;
        result = Utils.toMillis(duration, timeUnit);
        assertEquals(expResult, result);
    }

    @Test
    public void testValueCasting() throws IOException {
        /**
         * this just makes sure that we can cast from and to objects that only
         * have some of the same fields in common without throwing exceptions
         */
        Simple simple = new Simple("simple", 3);
        Complex complex = new Complex("test", 2, 0.5);

        String simpleJson = JSON.writeValueAsString(simple);
        JSON.readValue(simpleJson, Simple.class);

        String complexJSON = JSON.writeValueAsString(complex);
        JSON.readValue(complexJSON, Complex.class);

        simple = JSON.readValue(complexJSON, Simple.class);
        JSON.readValue(JSON.writeValueAsString(simple), Complex.class);
    }
}