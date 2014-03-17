package com.timpo.batphone.transports;

import com.timpo.batphone.handlers.Handler;

/**
 * Responsible for sending and receiving BinaryMessages
 */
public interface Transport<M> {

    /**
     * Indicates that messages sent to this topic should appear in the
     * onMessage callback.  
     * 
     * Must be called before start in order to work.
     *
     * @param topic
     */
    public void listenFor(String topic);

    /**
     * Any messages received by this transport should be passed to this handler
     *
     * @param handler
     */
    public void onMessage(Handler<M> handler);

    /**
     * Send a binary payload to the topic specified by the message's key
     *
     * @param message
     * @throws Exception
     */
    public void send(M message) throws Exception;

    /**
     * This transport should begin pulling in messages.
     *
     * Usually only called once
     */
    public void start();

    /**
     * This transport should stop pulling in messages.
     *
     * Usually only called once
     */
    public void stop();

    /**
     * This transport should close any resources it's using and prepare for the
     * process to exit.
     *
     * Once shut down a transport can't be restarted.
     */
    public void shutdown();
}
