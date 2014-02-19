package com.timpo.batphone.transports.mock;

import com.google.common.base.Optional;
import com.timpo.batphone.other.Constants;
import com.timpo.batphone.other.Utils;
import com.timpo.batphone.transports.AbstractTransport;
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

public class MockTransport extends AbstractTransport {

    private static final Logger LOG = Utils.logFor(MockTransport.class);
    //
    private final Set<String> channels;
    private final Set<Pattern> wildcardChannels;
    private final BlockingQueue<BinaryMessage> queue;

    public MockTransport(int numThreads, ExecutorService es) {
        super(numThreads, es);

        channels = new HashSet<>();
        wildcardChannels = new HashSet<>();
        queue = new LinkedBlockingQueue<>();
    }

    @Override
    protected List<? extends MessageConsumer> makeConsumers(Set<String> listenFor, int numThreads) {
        List<MockMessageConsumer> list = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            list.add(new MockMessageConsumer());
        }

        return list;
    }

    @Override
    public void send(BinaryMessage message) throws Exception {
        if (!channels.contains(message.getKey())) {
            boolean wildcardMatches = false;

            for (Pattern p : wildcardChannels) {
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
    public void listenFor(String channel) {
        super.listenFor(channel);

        channels.add(channel);
        if (Utils.isWildcard(channel)) {
            wildcardChannels.add(Pattern.compile(channel));
        }
    }

    private class MockMessageConsumer extends MessageConsumer {

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
