package com.timpo.batphone.transports;

import com.timpo.batphone.handlers.Handler;

/**
 * Responsible for sending and receiving BinaryMessages
 */
public interface Transport {

    /**
     * Indicates that messages sent to this channel should appear in the
     * onMessage callback
     *
     * @param channel
     */
    public void listenFor(String channel);

    /**
     * Any messages received by this transport should be passed to this handler
     *
     * @param handler
     */
    public void onMessage(Handler<BinaryMessage> handler);

    /**
     * Send a binary payload to the channel specifed by the message's key
     *
     * @param message
     * @throws Exception
     */
    public void send(BinaryMessage message) throws Exception;

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
