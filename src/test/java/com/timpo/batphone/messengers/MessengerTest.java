package com.timpo.batphone.messengers;

import com.google.common.util.concurrent.ListenableFuture;
import com.timpo.batphone.codecs.Codec;
import com.timpo.batphone.codecs.impl.JSONCodec;
import com.timpo.batphone.handlers.EventHandler;
import com.timpo.batphone.handlers.TypedRequestHandler;
import com.timpo.batphone.messages.Event;
import com.timpo.batphone.messages.Request;
import com.timpo.batphone.other.TestUtils;
import com.timpo.batphone.other.Utils;
import com.timpo.batphone.transports.Transport;
import com.timpo.batphone.transports.rabbit.RabbitAddress;
import com.timpo.batphone.transports.rabbit.RabbitPubSubTransport;
import com.timpo.batphone.transports.redis.RedisDirectTransport;
import com.timpo.batphone.holders.RoundRobinHolder;
import com.timpo.batphone.holders.BlockingQueueHolder;
import com.timpo.batphone.transports.TopicMessage;
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

        RoundRobinHolder<JedisPool> holder = new BlockingQueueHolder<>(new JedisPool("localhost"));
        Transport<TopicMessage<Request>> requestTransport = new RedisDirectTransport(codec, numThreads, es, holder);
        Transport<TopicMessage<Request>> responseTransport = new RedisDirectTransport(codec, numThreads, es, holder);

        Transport<TopicMessage<Event>> eventTransport = new RabbitPubSubTransport<>(codec, Event.class, numThreads, es, serviceGroup, new RabbitAddress());

        messenger = new MessengerImpl(serviceID, serviceGroup,
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
            String requestTopic = "test.req";

            messenger.onRequest(new TypedRequestHandler<Counter, Counter>(Counter.class) {
                @Override
                public Counter handle(Counter count, String topic) {
                    count.increment();

                    return count;
                }
            }, requestTopic);

            messenger.start();

            int startingCount = Utils.RAND.nextInt();
            int expectedCount = startingCount + 1;

            ListenableFuture<Request> response = messenger.request(new Counter(startingCount), requestTopic);

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
            String eventTopic = "test.event";

            final AtomicBoolean eventHandled = new AtomicBoolean(false);
            final int expectedCount = Utils.RAND.nextInt();

            messenger.onEvent(new EventHandler() {
                @Override
                public void handle(Event event, String topic) {
                    Assert.assertEquals(expectedCount, event.dataAs(Counter.class).getCount());
                    eventHandled.set(true);
                }
            }, eventTopic);

            messenger.start();

            messenger.notify(new Counter(expectedCount), eventTopic);

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