package com.timpo.batphone.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.base.Optional;
import com.timpo.batphone.handlers.RequestHandler;
import com.timpo.batphone.messages.Message;
import java.util.Map;

public class InstrumentedRequestHandler implements RequestHandler {

    private final RequestHandler wrappedHandler;
    private final Timer handlerTimer;
    private final Meter handlerMeter;

    public InstrumentedRequestHandler(RequestHandler wrappedHandler, Timer handlerTimer, Meter handlerMeter) {
        this.wrappedHandler = wrappedHandler;
        this.handlerTimer = handlerTimer;
        this.handlerMeter = handlerMeter;
    }

    @Override
    public Optional<Map<String, Object>> handle(Message request, String channel) {
        //track that this handler was called
        handlerMeter.mark();
        
        Timer.Context handlerTimerContext = handlerTimer.time();
        try {
            return wrappedHandler.handle(request, channel);
        } finally {
            handlerTimerContext.stop();
        }
    }
}
