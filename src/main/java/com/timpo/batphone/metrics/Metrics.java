package com.timpo.batphone.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import java.util.concurrent.TimeUnit;

public class Metrics {

    /**
     * Used to track performance metrics throughout the app
     */
    public static final MetricRegistry get = new MetricRegistry();

    public static ConsoleReporter makeReporter(int secondsBetweenReports) {
        ConsoleReporter reporter = ConsoleReporter.forRegistry(get)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();

        reporter.start(secondsBetweenReports, TimeUnit.SECONDS);

        return reporter;
    }
}
