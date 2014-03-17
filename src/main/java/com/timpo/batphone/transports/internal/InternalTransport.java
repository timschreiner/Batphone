package com.timpo.batphone.transports.internal;

import com.timpo.batphone.handlers.Handler;
import com.timpo.batphone.other.Utils;
import com.timpo.batphone.transports.polling.PollingTransport;
import com.timpo.batphone.transports.TopicMessage;
import com.timpo.batphone.transports.Transport;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;

public class InternalTransport implements Transport<TopicMessage> {

    private static final Logger LOG = Utils.logFor(PollingTransport.class);
    //
    private final Transport<TopicMessage> wrappedTransport;
    private final ExecutorService es;
    private final List<Handler<TopicMessage>> handlers;
    private final Map<String, Object> internalTopics;

    public InternalTransport(Transport<TopicMessage> wrappedTransport, ExecutorService es) {
        this.wrappedTransport = wrappedTransport;
        this.es = es;

        handlers = new CopyOnWriteArrayList<>();
        internalTopics = new ConcurrentHashMap<>();
    }

    @Override
    public void listenFor(String topic) {
        internalTopics.put(topic, null);

        wrappedTransport.listenFor(topic);
    }

    @Override
    public void onMessage(Handler<TopicMessage> handler) {
        handlers.add(handler);

        wrappedTransport.onMessage(handler);
    }

    @Override
    public void send(final TopicMessage message) throws Exception {
        //if this is being sent to a topic we listen for, handle it directly 
        //in a new thread
        if (internalTopics.containsKey(message.getTopic())) {
            
            es.submit(new Runnable() {
                @Override
                public void run() {
                    for (Handler<TopicMessage> handler : handlers) {
                        try {
                            handler.handle(message);

                        } catch (Exception ex) {
                            LOG.warn("handler encountered error", ex);
                        }
                    }
                }
            });

            //otherwise, just forward it to the wrappedSender
        } else {
            wrappedTransport.send(message);
        }
    }

    @Override
    public void start() {
        wrappedTransport.start();
    }

    @Override
    public void stop() {
        wrappedTransport.stop();
    }

    @Override
    public void shutdown() {
        wrappedTransport.shutdown();
    }
    /*
     * this class maintains a set of all the topics it's supposed to be listening 
     * to, and wraps another transport. everytime send is called, we first check the 
     * set to see if we already have a process listening for this type of message. 
     * if we do, we just use our threadpool to spawn a thread that calls onMessage 
     * with the message. otherwise, we just forward the message to the wrapped 
     * tranport's send().  we also need to to move encoding into the transports, 
     * so internal messages don't need to be needlessly encoded / decoded
     */
}
