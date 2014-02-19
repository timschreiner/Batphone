package com.timpo.batphone.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timpo.batphone.messengers.Messenger;
import com.timpo.batphone.other.Utils;
import com.yammer.metrics.json.MetricsModule;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

public class MessengerReporter extends ScheduledReporter {

    private static final Logger LOG = Utils.logFor(MessengerReporter.class);

    /**
     * Returns a new {@link Builder} for {@link MessengerReporter}.
     *
     * @param registry the registry to report
     * @Param messenger the messenger that will do the reporting
     * @return a {@link Builder} instance for a {@link MessengerReporter}
     */
    public static Builder forRegistry(MetricRegistry registry, Messenger messenger) {
        return new Builder(registry, messenger);
    }

    /**
     * A builder for {@link MessengerReporter} instances. Defaults to using the
     * default locale and time zone, converting rates to events/second,
     * converting durations to milliseconds, and not filtering metrics.
     */
    public static class Builder {

        private final MetricRegistry registry;
        private Messenger messenger;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;

        private Builder(MetricRegistry registry, Messenger messenger) {
            this.registry = registry;
            this.messenger = messenger;
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit a unit of time
         * @return {@code this}
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit a unit of time
         * @return {@code this}
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter a {@link MetricFilter}
         * @return {@code this}
         */
        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Builds a {@link MessengerReporter} with the given properties.
         *
         * @return a {@link MessengerReporter}
         */
        public MessengerReporter build() {
            return new MessengerReporter(registry,
                    messenger,
                    rateUnit,
                    durationUnit,
                    filter);
        }
    }
    private final Messenger messenger;
    private final ObjectMapper mapper;

    private MessengerReporter(MetricRegistry registry,
            Messenger messenger,
            TimeUnit rateUnit,
            TimeUnit durationUnit,
            MetricFilter filter) {

        super(registry, "messenger-reporter", filter, rateUnit, durationUnit);

        this.messenger = messenger;

        mapper = new ObjectMapper().registerModule(
                new MetricsModule(rateUnit, durationUnit, false));
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
            SortedMap<String, Counter> counters,
            SortedMap<String, Histogram> histograms,
            SortedMap<String, Meter> meters,
            SortedMap<String, Timer> timers) {

        notifyMetric(gauges, "gauge");
        notifyMetric(counters, "counters");
        notifyMetric(histograms, "histograms");
        notifyMetric(meters, "meters");
        notifyMetric(timers, "timers");
    }

    private void notifyMetric(SortedMap<String, ? extends Object> metric, String type) {
        if (!metric.isEmpty()) {
            for (Map.Entry<String, ? extends Object> entry : metric.entrySet()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = mapper.convertValue(entry.getValue(), Map.class);

                String to = "log.metrics." + type + ".[" + entry.getKey() + "]."
                        + messenger.getServiceGroup() + "." + messenger.getServiceID();

                try {
                    messenger.notify(data, to);

                } catch (Exception ex) {
                    LOG.warn("unable to send metric to messenger", ex);
                }
            }
        }
    }
}