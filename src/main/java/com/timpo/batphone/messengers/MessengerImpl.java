package com.timpo.batphone.messengers;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.timpo.batphone.handlers.EventHandler;
import com.timpo.batphone.handlers.Handler;
import com.timpo.batphone.handlers.RequestHandler;
import com.timpo.batphone.messages.Event;
import com.timpo.batphone.messages.Message;
import com.timpo.batphone.messages.Request;
import com.timpo.batphone.other.Utils;
import com.timpo.batphone.responsemappers.ResponseMapperImpl;
import com.timpo.batphone.responsemappers.ResponseMapper;
import com.timpo.batphone.transports.TopicMessage;
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
        public void handle(Event event, String topic) {
            LOG.warn("empty event handler called for {} : {}", topic, event);
        }
    };
    private static final RequestHandler EMPTY_REQUEST_HANDLER = new RequestHandler() {
        @Override
        public Optional<Map<String, Object>> handle(Request request, String topic) {
            LOG.warn("empty request handler called for {} : {}", topic, request);
            return Optional.absent();
        }
    };
    //
    private final String serviceID;
    private final String serviceGroup;
    private final ConcurrentMap<String, RequestHandler> requestHandlers;
    private final ConcurrentMap<String, EventHandler> eventHandlers;
    private final ConcurrentMap<Pattern, EventHandler> eventWildcardHandlers;
    private final Transport<TopicMessage<Request>> requestTransport;
    private final Transport<TopicMessage<Request>> responseTransport;
    private final Transport<TopicMessage<Event>> eventTransport;
    private final ResponseMapper<Request> responseMapper;

    public MessengerImpl(String serviceID, String serviceGroup,
            Transport<TopicMessage<Request>> requestTransport, Transport<TopicMessage<Request>> responseTransport,
            Transport<TopicMessage<Event>> eventTransport, ExecutorService es) throws Exception {

        this.serviceID = serviceID;
        this.serviceGroup = serviceGroup;
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
    public void onRequest(RequestHandler handler, String... topics) {
        validateTopics(topics);


        for (String topic : topics) {
            requestHandlers.put(topic, handler);

            requestTransport.listenFor(topic);
        }
    }

    @Override
    public void onEvent(EventHandler handler, String... topics) {
        validateTopics(topics);

        for (String topic : topics) {
            //even though wildcard topics can never be pulled directly, we 
            //have the optimization below that grabs the only thing in this map 
            //if only one entry exists, so we need to make sure this handler is 
            //in eventHandlers
            eventHandlers.put(topic, handler);

            //there's no need to specially track a topic that doesn't use wildcards
            if (Utils.isWildcard(topic)) {
                //TODO: do we need to modify the pattern if we know we're using 
                //rabbitmq, since it doesn't use regex?
                eventWildcardHandlers.put(Pattern.compile(topic), handler);
            }

            eventTransport.listenFor(topic);
        }
    }

    @Override
    public ListenableFuture<Request> request(Object dataObject, String... pipeline) throws Exception {
        if (pipeline.length == 0) {
            throw new IllegalArgumentException("pipeline must contain at least one destination");
        }

        for (String topic : pipeline) {
            if (topic == null || topic.isEmpty()) {
                throw new IllegalArgumentException("cannot pipe request to null or empty topic");
            }
        }

        Map<String, Object> data = Utils.convertToMap(dataObject);

        Request req = new Request();
        req.setFrom(serviceID);
        req.setTo(pipeline);
        req.setRequestID(Utils.simpleID());
        req.setData(data);

        //we do the remove so that the receiver doesn't see itself in the 'to' field
        String destination = req.getTo().remove(0);

        req.setTimeToNow();

        ListenableFuture<Request> future = responseMapper.makeFuture(req.getRequestID());

        requestTransport.send(new TopicMessage(destination, req));

        return future;
    }

    @Override
    public void notify(Object body, String... topics) throws Exception {
        if (topics.length == 0) {
            throw new IllegalArgumentException("topics must contain at least one topic");
        }

        Map<String, Object> data = Utils.convertToMap(body);

        Event event = new Event();
        event.setFrom(serviceID);
        event.setData(data);
        //event.to isn't set because events don't pipeline, so it's always empty
        event.setTimeToNow();

        //we do the remove so that the receiver doesn't see itself in the 'to' field
        for (String topic : topics) {
            eventTransport.send(new TopicMessage<>(topic, event));
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

    private RequestHandler getRequestHandler(TopicMessage<Request> cm) {
        //if there's only one request handler, just return it
        if (requestHandlers.size() == 1) {
            return requestHandlers.values().iterator().next();
        }

        //requests don't use wildcards, so we can always just check the map
        RequestHandler rh = requestHandlers.get(cm.getTopic());
        if (rh != null) {
            return rh;
        }

        return EMPTY_REQUEST_HANDLER;
    }

    private EventHandler getEventHandler(TopicMessage<Event> cm) {
        //if there's only one event handler, just return it: it will save lots 
        //of time, particularly with wildcards
        if (eventHandlers.size() == 1) {
            return eventHandlers.values().iterator().next();
        }

        EventHandler eh = eventHandlers.get(cm.getTopic());
        if (eh != null) {
            return eh;
        }

        //if we don't have this topic in our event handlers, it was likely a wildcard
        for (Map.Entry<Pattern, EventHandler> entry : eventWildcardHandlers.entrySet()) {
            Pattern pattern = entry.getKey();
            eh = entry.getValue();

            if (pattern.matcher(cm.getTopic()).matches()) {
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

    private void validateTopics(String[] topics) {
        if (topics.length == 0) {
            throw new IllegalArgumentException("cannot bind handler to empty topics list");
        }

        for (String topic : topics) {
            if (topic == null || topic.isEmpty()) {
                throw new IllegalArgumentException("cannot bind handler to null or empty topic");
            }
        }
    }

    private class MessengerRequestHandler implements Handler<TopicMessage<Request>> {

        private final Logger LOG = Utils.logFor(MessengerRequestHandler.class);

        @Override
        public void handle(TopicMessage<Request> cm) {
            LOG.debug("handle: {}", cm);

            try {
                Request request = cm.getMessage();

                RequestHandler rh = getRequestHandler(cm);

                Optional<Map<String, Object>> response = rh.handle(request, cm.getTopic());

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

                requestTransport.send(new TopicMessage<>(destination, request));

            } catch (Exception ex) {
                LOG.warn("problem handling request", ex);
            }
        }
    }

    private class MessengerResponseHandler implements Handler<TopicMessage<Request>> {

        private final Logger LOG = Utils.logFor(MessengerResponseHandler.class);

        @Override
        public void handle(TopicMessage<Request> cm) {
            LOG.debug("handle: {}", cm);

            try {
                //this is a response, since only they go directly to a serviceID
                responseMapper.resolveResponse(cm.getMessage());

            } catch (Exception ex) {
                LOG.warn("problem handling response", ex);
            }
        }
    }

    private class MessengerEventHandler implements Handler<TopicMessage<Event>> {

        private final Logger LOG = Utils.logFor(MessengerEventHandler.class);

        @Override
        public void handle(TopicMessage<Event> cm) {
            LOG.debug("handle: {}", cm);
            try {
                EventHandler eh = getEventHandler(cm);

                eh.handle(cm.getMessage(), cm.getTopic());

            } catch (Exception ex) {
                LOG.warn("problem handling event", ex);
            }
        }
    }
}
