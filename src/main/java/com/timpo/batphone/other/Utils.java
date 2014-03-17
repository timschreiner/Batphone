package com.timpo.batphone.other;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.io.BaseEncoding;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

    public static long timestamp() {
        return System.nanoTime() / 1000L; //current time in microseconds
    }
    public static String id = "";
    /**
     * Used to convert objects to and from Json
     */
    public static final ObjectMapper JSON = new ObjectMapper();

    static {
        //this ensures that we can decode arbitrary json to some class without 
        //it breaking when the json contains fields that the class does not
        JSON.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        //this ensures that fields with empty arrays aren't included in the 
        //message, which saves space
        JSON.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
    }
    /**
     * Used for generating random numbers
     */
    public static final Random RAND = new Random();
    //
    private static final String REQ_ID_PREFIX = "" + RAND.nextInt(10) + RAND.nextInt(10) + "-";
    private static final AtomicLong requestIDCounter = new AtomicLong(0);

    /**
     * @return an id that is unique to this instance
     */
    public static String simpleID() {
        //these really only need to be unique to this process since messages use 
        //the from field to send back to this one instance of the service
        return REQ_ID_PREFIX + requestIDCounter.incrementAndGet();
    }

    /**
     * @return an id that should be universally unique
     */
    public static String uniqueID() {
        UUID id = UUID.randomUUID();

        ByteBuffer bb = ByteBuffer.allocate(16);

        bb.putLong(id.getMostSignificantBits());
        bb.putLong(id.getLeastSignificantBits());

        return BaseEncoding.base64().encode(bb.array());
    }
    private static final AtomicLong STOP_WATCH = new AtomicLong(System.nanoTime());

    /**
     * Prints things with extra debugging information about the calling thread
     * and the time in microseconds since the last time threadDebugReset was
     * called
     *
     * @param o the thing to print
     */
    public static void debug(Object o) {
        threadPrint(o, false);
    }

    /**
     * Prints things with extra debugging information about the calling thread
     * and the time in microseconds since the last time threadDebugReset was
     * called, which will be 0 since this resets the timer before printing
     *
     * @param o
     */
    public static void debugReset(Object o) {
        threadPrint(o, true);
    }

    private static void threadPrint(Object o, boolean reset) {
        double timePassed = (System.nanoTime() - STOP_WATCH.get()) / 1000000.0;

        String threadName = Thread.currentThread().getName();

        if (reset) {
            STOP_WATCH.set(System.nanoTime());
            timePassed = 0;
        }

        String header = String.format("%-10s", threadName + " " + timePassed);

        System.out.println(header + ": " + o.toString());
    }

    /**
     * Converts a string to bytes using the project's Charset encoding
     *
     * @param string
     * @return the string's byte value
     */
    public static byte[] asBytes(String string) {
        return string.getBytes(Constants.ENCODING);
    }

    /**
     * Converts a byte array to a string project's Charset encoding
     *
     * @param bytes
     * @return the bytes' string value
     */
    public static String asString(byte[] bytes) {
        return new String(bytes, Constants.ENCODING);
    }

    /**
     * Converts objects to Maps. Useful for encoding classes into the simple
     * format used in Message data
     *
     * @param o
     * @return a Map of Strings to Objects
     */
    public static Map<String, Object> convertToMap(Object o) {
        if (o == null) {
            return Maps.newHashMap();
        }
        return (Map<String, Object>) JSON.convertValue(o, Map.class);
    }

    /**
     * Convenience method to be called at the end of a RequestHandler. Calling
     * this will enable the messenger to merge this objects fields with the
     * request Message's data fields. Note: this merge simply adds or overwrites
     * keys in the data map, so care must be taken when naming fields in a class
     * that names are unique unless they intend to overwrite other fields.
     *
     * @param o the object to be merged into the response Message
     * @return an Optional of the object converted to a Map
     */
    public static Optional<Map<String, Object>> response(Object o) {
        if (o == null) {
            throw new IllegalArgumentException("cannot respond with a null object; possibly use noResponse() instead");
        }

        return Optional.of(convertToMap(o));
    }

    /**
     * Determines if a topic contains wildcard characters
     *
     * @param topic the topic to check
     * @return true if the topic contains wildcard characters
     */
    public static boolean isWildcard(String topic) {
        return topic.matches(".*[^a-zA-Z0-9-_.].*");
    }

    /**
     * A convenience version of Thread.sleep that uses TimeUnits instead of raw
     * milliseconds and swallows interrupted exceptions
     *
     * @param duration
     * @param timeUnit
     */
    public static void sleep(long duration, TimeUnit timeUnit) {
        try {
            Thread.sleep(toMillis(duration, timeUnit));
        } catch (InterruptedException ex) {
            System.out.println("sleep interrupted: " + ex.getMessage());
        }
    }

    public static long secondsFromNow(long seconds) {
        return System.currentTimeMillis()
                + toMillis(seconds, TimeUnit.SECONDS);
    }

    public static boolean loopUntil(long endTime) {
        return endTime > System.currentTimeMillis();
    }

    /*
     * Get the millisecond representation of a time interval
     */
    public static long toMillis(long duration, TimeUnit timeUnit) {
        return TimeUnit.MILLISECONDS.convert(duration, timeUnit);
    }

    /*
     * Get a logging instance
     */
    public static Logger logFor(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
}
