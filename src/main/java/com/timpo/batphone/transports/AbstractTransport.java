package com.timpo.batphone.transports;

import com.google.common.base.Optional;
import com.timpo.batphone.handlers.Handler;
import com.timpo.batphone.other.Utils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;

public abstract class AbstractTransport implements Transport {

    private static final Logger LOG = Utils.logFor(AbstractTransport.class);
    //
    private final ExecutorService es;
    private final int numThreads;
    private final Set<String> listenFor;
    private final List<Handler<BinaryMessage>> handlers;
    private final AtomicBoolean consume;
    private final CountingLatch threadLatch;

    public AbstractTransport(int numThreads, ExecutorService es) {
        this.numThreads = numThreads;
        this.es = es;

        listenFor = new HashSet<>();
        handlers = new ArrayList<>();
        consume = new AtomicBoolean(true);
        threadLatch = new CountingLatch();
    }

    @Override
    public void listenFor(String channel) {
        listenFor.add(channel);
    }

    @Override
    public final void onMessage(Handler<BinaryMessage> handler) {
        handlers.add(handler);
    }

    @Override
    public final void start() {
        consume.set(true);

        if (!handlers.isEmpty() && !listenFor.isEmpty()) {
            List<? extends MessageConsumer> consumers = makeConsumers(listenFor, numThreads);
            if (consumers.size() != numThreads) {
                throw new RuntimeException("expected MessageConsumer count to be " + numThreads + ", not " + consumers.size());
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
     * Create MessageConsumers that perform the actual retrieval of messages for
     * the various transport implementations.
     *
     * The number of consumers should equal numThreads, or the transport could
     * block indefinitely.
     *
     * @param listenFor
     * @param numThreads
     * @return
     */
    protected abstract List<? extends MessageConsumer> makeConsumers(Set<String> listenFor, int numThreads);

    protected abstract class MessageConsumer implements Runnable {

        protected abstract Optional<BinaryMessage> nextMessage();

        @Override
        public void run() {
            while (consume.get()) {
                Optional<BinaryMessage> bm = nextMessage();

                if (bm.isPresent()) {
                    LOG.debug("handling message {}", bm.get());

                    for (Handler<BinaryMessage> handler : handlers) {
                        try {
                            handler.handle(bm.get());

                        } catch (Exception ex) {
                            LOG.warn("handler encountered error", ex);
                        }
                    }
                }
            }
        }
    }
}
