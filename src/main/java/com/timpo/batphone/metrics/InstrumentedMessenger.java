package com.timpo.batphone.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.ListenableFuture;
import com.timpo.batphone.handlers.EventHandler;
import com.timpo.batphone.handlers.RequestHandler;
import com.timpo.batphone.messages.Message;
import com.timpo.batphone.messengers.Messenger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InstrumentedMessenger {

    private static final Joiner channelJoiner = Joiner.on("_");
    //
    private final Messenger wrappedMessenger;
    private final ConcurrentMap<String, Timer> timers;

    public InstrumentedMessenger(Messenger wrappedMessenger) {
        this.wrappedMessenger = wrappedMessenger;

        timers = new ConcurrentHashMap<>();
    }

    public String getServiceID() {
        return wrappedMessenger.getServiceID();
    }

    public String getServiceGroup() {
        return wrappedMessenger.getServiceGroup();
    }

    public void onRequest(RequestHandler handler, String... channels) throws Exception {
        Timer timer = getHandlerTimerFor("onRequest", channels);

        Meter meter = getMeterFor("onRequest", channels);

        RequestHandler wrappedHandler = new InstrumentedRequestHandler(handler, timer, meter);

        wrappedMessenger.onRequest(wrappedHandler, channels);
    }

    public void onEvent(EventHandler handler, String... channels) throws Exception {
        Timer timer = getHandlerTimerFor("onEvent", channels);
        
        Meter meter = getMeterFor("onEvent", channels);

        EventHandler wrappedHandler = new InstrumentedEventHandler(handler, timer, meter);

        wrappedMessenger.onEvent(wrappedHandler, channels);
    }

    public ListenableFuture<Message> request(Object body, String... pipeline) throws Exception {
        Timer timer = getRequestTimerFor(pipeline);

        Timer.Context requestLatencyContext = timer.time();

        ListenableFuture<Message> rawFuture = wrappedMessenger.request(body, pipeline);

        return new InstrumentedFuture<>(rawFuture, requestLatencyContext);
    }

    public void notify(Object body, String... channels) throws Exception {
        wrappedMessenger.notify(body, channels);
    }

    public void start() throws Exception {
        wrappedMessenger.start();
    }

    public void stop() {
        wrappedMessenger.stop();
    }

    private Timer getHandlerTimerFor(String handlerType, String[] channels) {
        if (channels.length == 0) {
            throw new IllegalArgumentException("cannot bind handler to empty channels list");
        }

        return getTimer(handlerType, channels);
    }

    private Timer getRequestTimerFor(String[] pipeline) {
        if (pipeline.length == 0) {
            throw new IllegalArgumentException("pipeline must contain at least one destination");
        }

        return getTimer("req.latency", pipeline);
    }

    private Timer getTimer(String timerName, String[] channels) {
        String key = timerName + "." + channelJoiner.join(channels);
        Timer timer = timers.get(key);

        if (timer == null) {
            timers.putIfAbsent(key,
                    Metrics.get.timer("msgr." + key));

            timer = timers.get(key);
        }

        return timer;
    }

    private Meter getMeterFor(String name, String[] channels) {
        return Metrics.get.meter("msgr." + name + "." + channelJoiner.join(channels));
    }
}
