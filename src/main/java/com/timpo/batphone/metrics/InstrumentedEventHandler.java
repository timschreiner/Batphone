package com.timpo.batphone.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.timpo.batphone.handlers.EventHandler;
import com.timpo.batphone.messages.Message;

public class InstrumentedEventHandler implements EventHandler {

    private final EventHandler wrappedHandler;
    private final Timer handlerTimer;
    private final Meter meter;

    public InstrumentedEventHandler(EventHandler wrappedHandler, Timer handlerTimer, Meter meter) {
        this.wrappedHandler = wrappedHandler;
        this.handlerTimer = handlerTimer;
        this.meter = meter;
    }

    @Override
    public void handle(Message event, String channel) {
        meter.mark();
        
        Timer.Context handlerTimerContext = handlerTimer.time();
        try {
            wrappedHandler.handle(event, channel);

        } finally {
            handlerTimerContext.stop();
        }
    }
}
