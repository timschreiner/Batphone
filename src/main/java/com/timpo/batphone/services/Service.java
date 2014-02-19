package com.timpo.batphone.services;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.timpo.batphone.handlers.EventHandler;
import com.timpo.batphone.handlers.RequestHandler;
import com.timpo.batphone.messages.Message;
import com.timpo.batphone.messengers.Messenger;
import com.timpo.batphone.other.Utils;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;

public abstract class Service {
    
    private static final Logger LOG = Utils.logFor(Service.class);
    //
    private final Messenger messenger;
    private final ExecutorService es;
    protected final AtomicBoolean continueRunning = new AtomicBoolean(true);
    
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public Service(Messenger messenger, ServiceConfig cfg, ExecutorService es) throws Exception {
        this.messenger = messenger;
        this.es = es;

        //wire up this service to the messenger
        messenger.onEvent(new EventHandler() {
            @Override
            public void handle(Message event, String channel) {
                handleEvent(event, channel);
            }
        }, cfg.eventChannels);
        
        messenger.onRequest(new RequestHandler() {
            @Override
            public Optional<Map<String, Object>> handle(Message request, String channel) {
                return handleRequest(request, channel);
            }
        }, cfg.requestChannels);

        //TODO: wire up onRequest for handling commands sent to this service, like init / pause / continue / shutdown

        init();
        
        for (int i = 0; i < cfg.numRunThreads; i++) {
            es.submit(new Runnable() {
                @Override
                public void run() {
                    doWork();
                }
            });
        }
    }

    /**
     * Called only once before the service begins running. Resources used by the
     * service should be initialized here.
     */
    public void init() {
    }

    /**
     * Called when the service should begin running. Run may be called multiple
     * times throughout the lifecycle of the service.
     *
     * Executes in one or more of its own threads, so it is welcome to block,
     * although it might get interrupted if it's blocking while shutdown occurs.
     *
     * Code that should run regardless of incoming messages belongs here.
     */
    public void doWork() {
    }

    /**
     * Called when the service should pause its execution logic.
     *
     * Pause should not be allowed to block, since it executes in the container
     * thread.
     *
     * No new work will be given to the service until run is called again, and
     * any running code in the doWork() block should possibly cease.
     *
     * May be called multiple times throughout the lifecycle of the service, and
     * possibly while the service is already paused.
     */
    public void pause() {
    }

    /**
     * Called only once when the service should free any resources it is using
     * and prepare to be terminated.
     */
    public void shutdown() {
        continueRunning.set(false);
        
        try {
            //TODO: determine if this is the correct thing to try
            es.awaitTermination(1, TimeUnit.MINUTES);
            es.shutdown();
            
        } catch (InterruptedException ex) {
            LOG.warn("problem shutting down executor service", ex);
        }
    }

    /**
     * Runs when the service is notified of an Event
     *
     * @param event
     */
    public void handleEvent(Message event, String channel) {
        LOG.warn("handleEvent called without being overriden: channel={}", channel);
    }

    /**
     * Runs when the service receives a Request from another service.
     *
     * The Message's data Map can be modified directly, as it's contents will be
     * sent in the response or passed along to the next service in the pipe.
     * Alternatively, use Utils.respond(Object) to have that object's data
     * merged into the response for you
     *
     * @param request
     * @param channel
     * @return Optional data to be merged into the response data
     */
    public Optional<Map<String, Object>> handleRequest(Message request, String channel) {
        LOG.warn("handleRequest called without being overriden");
        return Optional.absent();
    }

    //MESSAGING
    /**
     * Used by the service to make requests of 1 or more services, in sequence.
     *
     * @param data
     * @param pipe
     * @return
     */
    protected ListenableFuture<Message> request(Map<String, Object> data, String... pipe) throws Exception {
        return messenger.request(data, pipe);
    }

    /**
     * Used by the service to notify other services of some Event
     *
     * @param channel
     * @param data
     */
    public void notify(String channel, Object data) throws Exception {
        messenger.notify(data, channel);
    }
}
