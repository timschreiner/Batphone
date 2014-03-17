package com.timpo.batphone.transports.polling;

import com.google.common.base.Optional;
import com.timpo.batphone.codecs.Codec;
import com.timpo.batphone.handlers.Handler;
import com.timpo.batphone.messages.Message;
import com.timpo.batphone.other.Utils;
import com.timpo.batphone.transports.BinaryMessage;
import com.timpo.batphone.transports.TopicMessage;
import com.timpo.batphone.transports.Transport;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;

/**
 * Handles most of the transport logic, making it easier to implement transports
 * that pull messages. Transports that have messages pushed to them likely won't
 * benefit as much from extending this class.
 */
public abstract class PollingTransport<M extends Message> implements Transport<TopicMessage<M>> {

    private static final Logger LOG = Utils.logFor(PollingTransport.class);
    //
    private final Codec codec;
    private final Class<M> decodeAs;
    private final int numThreads;
    private final ExecutorService es;
    private final Set<String> listenFor;
    private final List<Handler<TopicMessage<M>>> handlers;
    private final AtomicBoolean consume;
    private final CountingLatch threadLatch;

    public PollingTransport(Codec codec, Class<M> decodeAs, int numThreads, ExecutorService es) {
        this.codec = codec;
        this.decodeAs = decodeAs;
        this.numThreads = numThreads;
        this.es = es;

        listenFor = new HashSet<>();
        handlers = new CopyOnWriteArrayList<>();
        consume = new AtomicBoolean(true);
        threadLatch = new CountingLatch();
    }

    @Override
    public void listenFor(String topic) {
        listenFor.add(topic);
    }

    @Override
    public final void onMessage(Handler<TopicMessage<M>> handler) {
        handlers.add(handler);
    }

    @Override
    public final void start() {
        consume.set(true);

        if (!handlers.isEmpty() && !listenFor.isEmpty()) {
            List<? extends MessagePoller> consumers = makePollers(listenFor, numThreads);
            if (consumers.size() != numThreads) {
                throw new RuntimeException("expected MessagePoller count to be " + numThreads + ", not " + consumers.size());
            }

            for (final Runnable r : consumers) {
                es.submit(new Runnable() {
                    @Override
                    public void run() {
                        threadLatch.countUp();
                        try {
                            r.run();
                        } finally {
                            threadLatch.countDown();
                        }
                    }
                });
            }
        }
    }

    protected abstract void send(BinaryMessage bm) throws Exception;

    @Override
    final public void send(TopicMessage<M> cm) throws Exception {
        send(new BinaryMessage(cm.getTopic(), codec.encode(cm.getMessage())));
    }

    @Override
    public final void stop() {
        consume.set(false);

        try {
            threadLatch.await();

        } catch (InterruptedException ex) {
            LOG.warn("timed out while waiting for the MessageConsumers to finish", ex);
        }
    }

    @Override
    public void shutdown() {
        stop();
    }

    /**
     * Create MessagePollers that perform the actual retrieval of messages for
     * the various transport implementations.
     *
     * The number of pollers should equal numThreads, or the transport could
     * block indefinitely.
     *
     * @param listenFor
     * @param numThreads
     * @return
     */
    protected abstract List<? extends MessagePoller> makePollers(Set<String> listenFor, int numThreads);

    protected abstract class MessagePoller implements Runnable {

        protected abstract Optional<BinaryMessage> nextMessage();

        protected void shutdown() {
            //called in case the consumer needs to do any cleanup
        }

        @Override
        public void run() {
            while (consume.get()) {
                Optional<BinaryMessage> obm = nextMessage();

                if (obm.isPresent()) {
                    BinaryMessage bm = obm.get();
                    LOG.debug("handling message {}", bm);

                    try {
                        String key = bm.getKey();
                        M m = codec.decode(bm.getPayload(), decodeAs);
                        TopicMessage<M> cm = new TopicMessage<>(key, m);

                        for (Handler<TopicMessage<M>> handler : handlers) {
                            try {
                                handler.handle(cm);

                            } catch (Exception ex) {
                                LOG.warn("handler encountered error", ex);
                            }
                        }

                    } catch (IOException ex) {
                        LOG.warn("problem decoding message", ex);
                    }
                }
            }

            shutdown();
        }
    }
}
