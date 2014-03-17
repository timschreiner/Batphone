package com.timpo.batphone.transports.mock;

import com.google.common.base.Optional;
import com.timpo.batphone.codecs.Codec;
import com.timpo.batphone.messages.Message;
import com.timpo.batphone.other.Constants;
import com.timpo.batphone.other.Utils;
import com.timpo.batphone.transports.polling.PollingTransport;
import com.timpo.batphone.transports.BinaryMessage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;

public class MockTransport<M extends Message> extends PollingTransport<M> {

    private static final Logger LOG = Utils.logFor(MockTransport.class);
    //
    private final Set<String> topics;
    private final Set<Pattern> wildcardTopics;
    private final BlockingQueue<BinaryMessage> queue;

    public MockTransport(Codec codec, Class<M> decodeAs, int numThreads, ExecutorService es) {
        super(codec, decodeAs, numThreads, es);

        topics = new HashSet<>();
        wildcardTopics = new HashSet<>();
        queue = new LinkedBlockingQueue<>();
    }

    @Override
    protected List<? extends MessagePoller> makePollers(Set<String> listenFor, int numThreads) {
        List<MockMessageConsumer> list = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            list.add(new MockMessageConsumer());
        }

        return list;
    }

    @Override
    public void send(BinaryMessage message) throws Exception {
        if (!topics.contains(message.getKey())) {
            boolean wildcardMatches = false;

            for (Pattern p : wildcardTopics) {
                if (p.matcher(message.getKey()).matches()) {
                    wildcardMatches = true;
                    break;
                }
            }

            if (!wildcardMatches) {
                return;
            }
        }

        queue.add(message);
    }

    @Override
    public void listenFor(String topic) {
        super.listenFor(topic);

        topics.add(topic);
        if (Utils.isWildcard(topic)) {
            wildcardTopics.add(Pattern.compile(topic));
        }
    }

    private class MockMessageConsumer extends MessagePoller {

        @Override
        protected Optional<BinaryMessage> nextMessage() {
            try {
                return Optional.of(queue.poll(Constants.BLOCKING_TIMEOUT, TimeUnit.MILLISECONDS));

            } catch (InterruptedException ex) {
                LOG.warn("interrupted while getting the next message", ex);

                return Optional.absent();
            }
        }
    }
}
