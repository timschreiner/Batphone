package com.timpo.batphone.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import java.util.concurrent.TimeUnit;

public class Metrics {

    /**
     * Used to track performance metrics throughout the app
     */
    public static final MetricRegistry get = new MetricRegistry();

    public static void clear() {
        get.removeMatching(MetricFilter.ALL);
    }

    public static void report() {
        makeReporter().report();
    }

    public static ConsoleReporter makeReporter(int secondsBetweenReports) {
        ConsoleReporter reporter = makeReporter();

        reporter.start(secondsBetweenReports, TimeUnit.SECONDS);

        return reporter;
    }

    public static ConsoleReporter makeReporter() {
        return makeReporter(TimeUnit.MILLISECONDS);
    }

    public static ConsoleReporter makeReporter(TimeUnit convertDurationsTo) {
        ConsoleReporter reporter = ConsoleReporter.forRegistry(get)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(convertDurationsTo)
                .build();

        return reporter;
    }
}
