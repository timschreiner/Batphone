package com.timpo.batphone.transports;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.timpo.batphone.codecs.Codec;
import com.timpo.batphone.codecs.impl.JSONCodec;
import com.timpo.batphone.handlers.Handler;
import com.timpo.batphone.messages.Request;
import com.timpo.batphone.other.TestUtils;
import com.timpo.batphone.transports.mock.MockTransport;
import com.timpo.batphone.transports.rabbit.RabbitAddress;
import com.timpo.batphone.transports.rabbit.RabbitPubSubTransport;
import com.timpo.batphone.transports.redis.RedisDirectTransport;
import com.timpo.batphone.holders.RoundRobinHolder;
import com.timpo.batphone.holders.BlockingQueueHolder;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.JedisPool;

public class TransporterTest {

    private static final Request payload = new Request();
    private final Codec codec = new JSONCodec();
    private Transport<TopicMessage<Request>> transporter;
    private RoundRobinHolder<JedisPool> holder;
    private ExecutorService es;
    int numThreads;

    public TransporterTest() {
        holder = new BlockingQueueHolder<>(new JedisPool("localhost"));
        es = Executors.newCachedThreadPool();
        numThreads = 2;

        Map<String, Object> data = Maps.newHashMap();
        data.put("int", 1);
        data.put("string", "stuff");
        data.put("list", Lists.newArrayList(1, "b"));

        payload.setData(data);
        payload.setFrom("f1", "f2");
        payload.setRequestID("test-request-id");
        payload.setTo("t1", "t2");
    }

    @After
    public void tearDown() {
        transporter.stop();
    }

    @Test
    public void testBasicFunctionality() throws Exception {
        String receiveTopic = "test1.direct";
        final String sentTopic = receiveTopic;

        transporter = getDirectTransporter();
        runCommonTests(receiveTopic, sentTopic, payload);
        transporter.stop();

        transporter = getWildcardTransporter();
        runCommonTests(receiveTopic, sentTopic, payload);
        transporter.stop();

        transporter = new MockTransport<>(codec, Request.class, 1, Executors.newCachedThreadPool());
        runCommonTests(receiveTopic, sentTopic, payload);
        transporter.stop();
    }

    @Test
    public void testWildcards() throws Exception {
        String receiveTopic = "test2.*";
        final String sentTopic = "test2.test";

        transporter = getWildcardTransporter();
        runCommonTests(receiveTopic, sentTopic, payload);
        transporter.stop();

        transporter = new MockTransport<>(codec, Request.class, 1, Executors.newCachedThreadPool());
        runCommonTests(receiveTopic, sentTopic, payload);
        transporter.stop();
    }

    @SuppressWarnings("unchecked")
    private void runCommonTests(String recieveTopic, final String sendTopic, final Request payload) throws Exception, InterruptedException {
        final AtomicBoolean handleFired = new AtomicBoolean(false);
        final AtomicBoolean badTopicIgnored = new AtomicBoolean(true);

        final String badTopic = "poison";

        transporter.listenFor(recieveTopic);

        transporter.onMessage(new Handler<TopicMessage<Request>>() {
            @Override
            public void handle(TopicMessage<Request> mwk) {
                boolean topicsMatch = sendTopic.equals(mwk.getTopic());
                boolean payloadsMatch = payload.equals(mwk.getMessage());

                handleFired.set(topicsMatch && payloadsMatch);

                if (mwk.getTopic().equals(badTopic)) {
                    badTopicIgnored.set(false);
                }
            }
        });

        transporter.start();
        
        transporter.send(new TopicMessage<>(badTopic, payload));

        transporter.send(new TopicMessage<>(sendTopic, payload));

        TestUtils.failUnlessConditionMetWithin(handleFired, 2, TimeUnit.SECONDS);

        Assert.assertTrue(badTopicIgnored.get());
    }

    private Transport<TopicMessage<Request>> getDirectTransporter() {
        return new RedisDirectTransport(codec, numThreads, es, holder);
    }

//    private Transport getWildcardTransporter() {
//        return new KafkaPubSubTransport(numThreads, es, "test_group",
//                Lists.newArrayList(new BrokerAddress()),
//                Lists.newArrayList(new ZookeeperAddress()));
//    }
    private Transport<TopicMessage<Request>> getWildcardTransporter() throws IOException {
        return new RabbitPubSubTransport<>(codec, Request.class, numThreads, es, "test_group", new RabbitAddress());
    }
}