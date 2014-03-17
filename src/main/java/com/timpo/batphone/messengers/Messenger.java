package com.timpo.batphone.messengers;

import com.google.common.util.concurrent.ListenableFuture;
import com.timpo.batphone.handlers.EventHandler;
import com.timpo.batphone.handlers.RequestHandler;
import com.timpo.batphone.messages.Request;

/**
 * Manages the creation of messages, compression, encoding, transmission and the
 * request/response pattern
 */
public interface Messenger {

    /**
     * @return the ID for this unique instance of a service. Responses will be
     * sent here
     */
    public String getServiceID();

    /**
     * @return the name of this type of service. service instances in the same
     * service group have their messages load-balanced between them
     */
    public String getServiceGroup();

    /**
     * Bind a handler to 1 or more request topics
     *
     * @param handler
     * @param topics
     * @throws Exception
     */
    public void onRequest(RequestHandler handler, String... topics);

    /**
     * Bind a handler to 1 or more event topics
     *
     * @param handler
     * @param topics
     * @throws Exception
     */
    public void onEvent(EventHandler handler, String... topics);

    /**
     * Send any object as a request to 1 or more destination services.
     *
     * Messages are sent to the first destination in a pipeline, then that
     * service passes it along to the next, etc., until the pipeline is empty,
     * at which point the response is sent back to this messenger.
     *
     * @param body the data for the request
     * @param pipeline 1 or more destinations this request should be passed
     * through
     * @return a ListenableFuture that can be used to retrieve the response to
     * this request
     * @throws Exception if something has broken with the underlying transports
     * that back this implementation
     */
    public ListenableFuture<Request> request(Object body, String... pipeline) throws Exception;

    /**
     * Notify subscribed services about an event occurring.
     *
     * @param body the data for the event
     * @param topics 1 or more topics that describe or contain events like
     * this
     * @throws Exception if something has broken with the underlying transports
     * that back this implementation
     */
    public void notify(Object body, String... topics) throws Exception;

    /**
     * This messenger should begin pulling in and handling messages.
     *
     * Usually only called once
     */
    public void start();

    /**
     * This messenger should stop pulling in and handling messages.
     *
     * Usually only called once
     */
    public void stop();

    /**
     * This messenger should close any resources it's using and prepare for the
     * process to exit.
     *
     * Once shut down a messenger can't be restarted.
     */
    public void shutdown();
}
