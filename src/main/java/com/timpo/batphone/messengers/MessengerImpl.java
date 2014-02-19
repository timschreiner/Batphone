package com.timpo.batphone.messengers;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.timpo.batphone.codecs.Codec;
import com.timpo.batphone.handlers.EventHandler;
import com.timpo.batphone.handlers.Handler;
import com.timpo.batphone.handlers.RequestHandler;
import com.timpo.batphone.messages.Message;
import com.timpo.batphone.other.Utils;
import com.timpo.batphone.responsemappers.ResponseMapperImpl;
import com.timpo.batphone.responsemappers.ResponseMapper;
import com.timpo.batphone.transports.BinaryMessage;
import com.timpo.batphone.transports.Transport;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import org.slf4j.Logger;

public class MessengerImpl implements Messenger {

    private static final Logger LOG = Utils.logFor(MessengerImpl.class);
    private static final EventHandler EMPTY_EVENT_HANDLER = new EventHandler() {
        @Override
        public void handle(Message event, String channel) {
            LOG.warn("empty event handler called for {} : {}", channel, event);
        }
    };
    private static final RequestHandler EMPTY_REQUEST_HANDLER = new RequestHandler() {
        @Override
        public Optional<Map<String, Object>> handle(Message request, String channel) {
            LOG.warn("empty request handler called for {} : {}", channel, request);
            return Optional.absent();
        }
    };
    //
    private final String serviceID;
    private final String serviceGroup;
    private final ConcurrentMap<String, RequestHandler> requestHandlers;
    private final ConcurrentMap<String, EventHandler> eventHandlers;
    private final ConcurrentMap<Pattern, EventHandler> eventWildcardHandlers;
    private final Codec codec;
    private final Transport requestTransport;
    private final Transport responseTransport;
    private final Transport eventTransport;
    private final ResponseMapper<Message> responseMapper;

    public MessengerImpl(String serviceID, String serviceGroup, Codec codec,
            Transport requestTransport, Transport responseTransport,
            Transport eventTransport, ExecutorService es) throws Exception {

        this.serviceID = serviceID;
        this.serviceGroup = serviceGroup;
        this.codec = codec;
        this.requestTransport = requestTransport;
        this.responseTransport = responseTransport;
        this.eventTransport = eventTransport;

        requestHandlers = new ConcurrentHashMap<>();
        eventHandlers = new ConcurrentHashMap<>();
        eventWildcardHandlers = new ConcurrentHashMap<>();

        responseMapper = new ResponseMapperImpl(es);

        //responses are sent to the service id
        this.responseTransport.listenFor(serviceID);

        this.requestTransport.onMessage(new MessengerRequestHandler());
        this.responseTransport.onMessage(new MessengerResponseHandler());
        this.eventTransport.onMessage(new MessengerEventHandler());
    }

    @Override
    public String getServiceID() {
        return serviceID;
    }

    @Override
    public String getServiceGroup() {
        return serviceGroup;
    }

    @Override
    public void onRequest(RequestHandler handler, String... channels) {
        if (channels.length == 0) {
            throw new IllegalArgumentException("cannot bind handler to empty channels list");
        }

        for (String channel : channels) {
            requestHandlers.put(channel, handler);

            requestTransport.listenFor(channel);
        }
    }

    @Override
    public void onEvent(EventHandler handler, String... channels) {
        if (channels.length == 0) {
            throw new IllegalArgumentException("cannot bind handler to empty channels list");
        }

        for (String channel : channels) {
            //even though wildcard channels can never be pulled directly, we 
            //have the optimization below that grabs the only thing in this map 
            //if only one entry exists, so we need to make sure this handler is 
            //in eventHandlers
            eventHandlers.put(channel, handler);

            //there's no need to specially track a channel that doesn't use wildcards
            if (Utils.isWildcard(channel)) {
                //TODO: do we need to modify the pattern if we know we're using 
                //rabbitmq, since it doesn't use regex?
                eventWildcardHandlers.put(Pattern.compile(channel), handler);
            }

            eventTransport.listenFor(channel);
        }
    }

    @Override
    public ListenableFuture<Message> request(Object dataObject, String... pipeline) throws Exception {
        if (pipeline.length == 0) {
            throw new IllegalArgumentException("pipeline must contain at least one destination");
        }

        Map<String, Object> data = Utils.convertToMap(dataObject);

        Message req = new Message();
        req.setFrom(serviceID);
        req.setTo(pipeline);
        req.setRequestID(Utils.simpleID());
        req.setData(data);

        //we do the remove so that the receiver doesn't see itself in the 'to' field
        String destination = req.getTo().remove(0);

        req.setTimeToNow();

        byte[] encodedMessage = codec.encode(req);

        ListenableFuture<Message> future = responseMapper.makeFuture(req.getRequestID());

        requestTransport.send(new BinaryMessage(destination, encodedMessage));

        return future;
    }

    @Override
    public void notify(Object body, String... channels) throws Exception {
        if (channels.length == 0) {
            throw new IllegalArgumentException("channels must contain at least one channel");
        }

        Map<String, Object> data = Utils.convertToMap(body);

        Message event = new Message();
        event.setFrom(serviceID);
        event.setData(data);
        //event.to isn't set because events don't pipeline, so it's always empty
        event.setTimeToNow();

        //we do the remove so that the receiver doesn't see itself in the 'to' field
        byte[] encodedMessage = codec.encode(event);

        for (String channel : channels) {
            eventTransport.send(new BinaryMessage(channel, encodedMessage));
        }
    }

    @Override
    public void start() {
        requestTransport.start();
        responseTransport.start();
        eventTransport.start();
    }

    @Override
    public void stop() {
        requestTransport.stop();
        responseTransport.stop();
        eventTransport.stop();
    }

    private void mergeData(Message req, Map<String, Object> resData) {
        for (Map.Entry<String, Object> entry : resData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            req.getData().put(key, value);
        }
    }

    private RequestHandler getRequestHandler(BinaryMessage bm) {
        //if there's only one request handler, just return it
        if (requestHandlers.size() == 1) {
            return requestHandlers.values().iterator().next();
        }

        //requests don't use wildcards, so we can always just check the map
        RequestHandler rh = requestHandlers.get(bm.getKey());
        if (rh != null) {
            return rh;
        }

        return EMPTY_REQUEST_HANDLER;
    }

    private EventHandler getEventHandler(BinaryMessage bm) {
        //if there's only one event handler, just return it: it will save lots 
        //of time, particularly with wildcards
        if (eventHandlers.size() == 1) {
            return eventHandlers.values().iterator().next();
        }

        EventHandler eh = eventHandlers.get(bm.getKey());
        if (eh != null) {
            return eh;
        }

        //if we don't have this channel in our event handlers, it was likely a wildcard
        for (Map.Entry<Pattern, EventHandler> entry : eventWildcardHandlers.entrySet()) {
            Pattern pattern = entry.getKey();
            eh = entry.getValue();

            if (pattern.matcher(bm.getKey()).matches()) {
                return eh;
            }
        }

        return EMPTY_EVENT_HANDLER;
    }

    @Override
    public void shutdown() {
        stop();

        requestTransport.shutdown();
        responseTransport.shutdown();
        eventTransport.shutdown();

    }

    private class MessengerRequestHandler implements Handler<BinaryMessage> {

        private final Logger LOG = Utils.logFor(MessengerRequestHandler.class);

        @Override
        public void handle(BinaryMessage bm) {
            LOG.debug("handle: {}", bm);

            try {
                Message request = codec.decode(bm.getPayload(), Message.class);

                RequestHandler rh = getRequestHandler(bm);

                Optional<Map<String, Object>> response = rh.handle(request, bm.getKey());

                //some requests might have their response data merged in 
                //directly, otherwise we can merge it in
                if (response.isPresent()) {
                    mergeData(request, response.get());
                }


                String destination;
                if (!request.getTo().isEmpty()) {
                    //either forward this request to the next destination in 
                    //the 'to' field...    
                    destination = request.getTo().remove(0);
                    //remove it so the receiver doesn't see themselves in the 'to'
                    //field and cause a loop

                } else {
                    //... or respond back to the original sender
                    destination = request.getFrom().get(0);
                }

                request.setTimeToNow();

                byte[] encodedMessage = codec.encode(request);

                requestTransport.send(new BinaryMessage(destination, encodedMessage));

            } catch (Exception ex) {
                LOG.warn("problem handling request", ex);
            }
        }
    }

    private class MessengerResponseHandler implements Handler<BinaryMessage> {

        private final Logger LOG = Utils.logFor(MessengerResponseHandler.class);

        @Override
        public void handle(BinaryMessage bm) {
            LOG.debug("handle: {}", bm);

            try {
                Message m = codec.decode(bm.getPayload(), Message.class);

                //this is a response, since only they go directly to a serviceID
                responseMapper.resolveResponse(m);

            } catch (Exception ex) {
                LOG.warn("problem handling response", ex);
            }
        }
    }

    private class MessengerEventHandler implements Handler<BinaryMessage> {

        private final Logger LOG = Utils.logFor(MessengerEventHandler.class);

        @Override
        public void handle(BinaryMessage bm) {
            LOG.debug("handle: {}", bm);
            try {
                Message m = codec.decode(bm.getPayload(), Message.class);

                EventHandler eh = getEventHandler(bm);

                eh.handle(m, bm.getKey());

            } catch (Exception ex) {
                LOG.warn("problem handling event", ex);
            }
        }
    }
}
