package com.timpo.batphone.transports;

import com.timpo.batphone.handlers.Handler;
import com.timpo.batphone.other.TestUtils;
import com.timpo.batphone.other.Utils;
import com.timpo.batphone.transports.mock.MockTransport;
import com.timpo.batphone.transports.rabbit.RabbitAddress;
import com.timpo.batphone.transports.rabbit.RabbitPubSubTransport;
import com.timpo.batphone.transports.redis.RedisDirectTransport;
import com.timpo.batphone.transports.redis.RoundRobinHolder;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.JedisPool;

public class TransporterTest {

    private Transport transporter;
    private RoundRobinHolder<JedisPool> holder;
    private ExecutorService es;
    int numThreads;

    public TransporterTest() {
        holder = new RoundRobinHolder<>(new JedisPool("localhost"));
        es = Executors.newCachedThreadPool();
        numThreads = 2;
    }

        @After
        public void tearDown() {
            transporter.stop();
        }

    @Test
    public void testBasicFunctionality() throws Exception {
        String receiveChannel = "test1.direct";
        final String sentChannel = receiveChannel;
        final byte[] payload = Utils.asBytes("hello");

        transporter = getDirectTransporter();
        runCommonTests(receiveChannel, sentChannel, payload);
        transporter.stop();

        transporter = getWildcardTransporter();
        runCommonTests(receiveChannel, sentChannel, payload);
        transporter.stop();

        transporter = new MockTransport(1, Executors.newCachedThreadPool());
        runCommonTests(receiveChannel, sentChannel, payload);
        transporter.stop();
    }

    @Test
    public void testWildcards() throws Exception {
        String receiveChannel = "test2.*";
        final String sentChannel = "test2.test";
        final byte[] payload = Utils.asBytes("hello-" + new Date());

        transporter = getWildcardTransporter();
        runCommonTests(receiveChannel, sentChannel, payload);
        transporter.stop();

        transporter = new MockTransport(1, Executors.newCachedThreadPool());
        runCommonTests(receiveChannel, sentChannel, payload);
        transporter.stop();
    }

    private void runCommonTests(String recieveChannel, final String sendChannel, final byte[] payload) throws Exception, InterruptedException {
        final AtomicBoolean handleFired = new AtomicBoolean(false);
        final AtomicBoolean badChannelIgnored = new AtomicBoolean(true);

        final String badChannel = "poison";

        transporter.listenFor(recieveChannel);

        transporter.onMessage(new Handler<BinaryMessage>() {
            @Override
            public void handle(BinaryMessage bm) {
                boolean channelsMatch = sendChannel.equals(bm.getKey());
                boolean payloadsMatch = Arrays.equals(payload, bm.getPayload());

                handleFired.set(channelsMatch && payloadsMatch);

                if (bm.getKey().equals(badChannel)) {
                    badChannelIgnored.set(false);
                }
            }
        });

        transporter.start();

        transporter.send(new BinaryMessage(badChannel, payload));

        transporter.send(new BinaryMessage(sendChannel, payload));

        TestUtils.failUnlessConditionMetWithin(handleFired, 2, TimeUnit.SECONDS);

        Assert.assertTrue(badChannelIgnored.get());
    }

    private Transport getDirectTransporter() {
        return new RedisDirectTransport(numThreads, es, holder);
    }

//    private Transport getWildcardTransporter() {
//        return new KafkaPubSubTransport(numThreads, es, "test_group",
//                Lists.newArrayList(new BrokerAddress()),
//                Lists.newArrayList(new ZookeeperAddress()));
//    }
    private Transport getWildcardTransporter() throws IOException {
        return new RabbitPubSubTransport(numThreads, es, "test_group", new RabbitAddress());
    }
}