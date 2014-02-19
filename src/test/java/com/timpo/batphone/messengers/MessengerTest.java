package com.timpo.batphone.messengers;

import com.google.common.util.concurrent.ListenableFuture;
import com.timpo.batphone.codecs.Codec;
import com.timpo.batphone.codecs.JSONCodec;
import com.timpo.batphone.handlers.EventHandler;
import com.timpo.batphone.handlers.TypedRequestHandler;
import com.timpo.batphone.messages.Message;
import com.timpo.batphone.other.TestUtils;
import com.timpo.batphone.other.Utils;
import com.timpo.batphone.transports.Transport;
import com.timpo.batphone.transports.rabbit.RabbitAddress;
import com.timpo.batphone.transports.rabbit.RabbitPubSubTransport;
import com.timpo.batphone.transports.redis.RedisDirectTransport;
import com.timpo.batphone.transports.redis.RoundRobinHolder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.JedisPool;

public class MessengerTest {
    //TODO: find a better way to make a mock transport, or change this test to use the real transports

    private Messenger messenger;

    @Before
    public void setUp() throws Exception {
        String serviceGroup = "test_service";
        String serviceID = serviceGroup + "." + Utils.simpleID();

        Codec codec = new JSONCodec();
        int numThreads = 1;
        ExecutorService es = Executors.newCachedThreadPool();

        RoundRobinHolder<JedisPool> holder = new RoundRobinHolder<>(new JedisPool("localhost"));
        Transport requestTransport = new RedisDirectTransport(numThreads, es, holder);
        Transport responseTransport = new RedisDirectTransport(numThreads, es, holder);

        Transport eventTransport = new RabbitPubSubTransport(numThreads, es, serviceGroup, new RabbitAddress());

        messenger = new MessengerImpl(serviceID, serviceGroup, codec,
                requestTransport,
                responseTransport,
                eventTransport,
                es);
    }

    @After
    public void tearDown() {
        messenger.stop();
    }

    @Test
    public void testRequest() {
        try {
            String requestChannel = "test.req";

            messenger.onRequest(new TypedRequestHandler<Counter, Counter>(Counter.class) {
                @Override
                public Counter handle(Counter count, String channel) {
                    count.increment();

                    return count;
                }
            }, requestChannel);

            messenger.start();

            int startingCount = Utils.RAND.nextInt();
            int expectedCount = startingCount + 1;

            ListenableFuture<Message> response = messenger.request(new Counter(startingCount), requestChannel);

            Counter count = response.get(500, TimeUnit.MILLISECONDS).dataAs(Counter.class);

            Assert.assertEquals(count.getCount(), expectedCount);

        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testEvent() {
        try {
            String eventChannel = "test.event";

            final AtomicBoolean eventHandled = new AtomicBoolean(false);
            final int expectedCount = Utils.RAND.nextInt();

            messenger.onEvent(new EventHandler() {
                @Override
                public void handle(Message event, String channel) {
                    Assert.assertEquals(expectedCount, event.dataAs(Counter.class).getCount());
                    eventHandled.set(true);
                }
            }, eventChannel);

            messenger.start();

            messenger.notify(new Counter(expectedCount), eventChannel);

            TestUtils.failUnlessConditionMetWithin(eventHandled, 1, TimeUnit.SECONDS);

        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail();
        }
    }

    private static class Counter {

        private int count;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public Counter(int count) {
            this.count = count;
        }

        public Counter() {
        }

        @Override
        public String toString() {
            return "Count{" + "count=" + count + '}';
        }

        private int increment() {
            return count++;
        }
    }
}