package com.timpo.batphone.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.ListenableFuture;
import com.timpo.batphone.handlers.EventHandler;
import com.timpo.batphone.handlers.RequestHandler;
import com.timpo.batphone.messages.Message;
import com.timpo.batphone.messages.Request;
import com.timpo.batphone.messengers.Messenger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InstrumentedMessenger {

    private static final Joiner topicJoiner = Joiner.on("_");
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

    public void onRequest(RequestHandler handler, String... topics) throws Exception {
        Timer timer = getHandlerTimerFor("onRequest", topics);

        Meter meter = getMeterFor("onRequest", topics);

        RequestHandler wrappedHandler = new InstrumentedRequestHandler(handler, timer, meter);

        wrappedMessenger.onRequest(wrappedHandler, topics);
    }

    public void onEvent(EventHandler handler, String... topics) throws Exception {
        Timer timer = getHandlerTimerFor("onEvent", topics);
        
        Meter meter = getMeterFor("onEvent", topics);

        EventHandler wrappedHandler = new InstrumentedEventHandler(handler, timer, meter);

        wrappedMessenger.onEvent(wrappedHandler, topics);
    }

    public ListenableFuture<Request> request(Object body, String... pipeline) throws Exception {
        Timer timer = getRequestTimerFor(pipeline);

        Timer.Context requestLatencyContext = timer.time();

        ListenableFuture<Request> rawFuture = wrappedMessenger.request(body, pipeline);

        return new InstrumentedFuture<>(rawFuture, requestLatencyContext);
    }

    public void notify(Object body, String... topics) throws Exception {
        wrappedMessenger.notify(body, topics);
    }

    public void start() throws Exception {
        wrappedMessenger.start();
    }

    public void stop() {
        wrappedMessenger.stop();
    }

    private Timer getHandlerTimerFor(String handlerType, String[] topics) {
        if (topics.length == 0) {
            throw new IllegalArgumentException("cannot bind handler to empty topics list");
        }

        return getTimer(handlerType, topics);
    }

    private Timer getRequestTimerFor(String[] pipeline) {
        if (pipeline.length == 0) {
            throw new IllegalArgumentException("pipeline must contain at least one destination");
        }

        return getTimer("req.latency", pipeline);
    }

    private Timer getTimer(String timerName, String[] topics) {
        String key = timerName + "." + topicJoiner.join(topics);
        Timer timer = timers.get(key);

        if (timer == null) {
            timers.putIfAbsent(key,
                    Metrics.get.timer("msgr." + key));

            timer = timers.get(key);
        }

        return timer;
    }

    private Meter getMeterFor(String name, String[] topics) {
        return Metrics.get.meter("msgr." + name + "." + topicJoiner.join(topics));
    }
}
