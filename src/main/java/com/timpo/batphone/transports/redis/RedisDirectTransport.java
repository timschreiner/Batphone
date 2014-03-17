package com.timpo.batphone.transports.redis;

import com.timpo.batphone.holders.RoundRobinHolder;
import com.google.common.base.Optional;
import com.timpo.batphone.codecs.Codec;
import com.timpo.batphone.holders.HolderFactory;
import com.timpo.batphone.messages.Request;
import com.timpo.batphone.other.Constants;
import com.timpo.batphone.other.Utils;
import com.timpo.batphone.transports.polling.PollingTransport;
import com.timpo.batphone.transports.BinaryMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisDirectTransport extends PollingTransport<Request> {

    private static final Logger LOG = Utils.logFor(RedisDirectTransport.class);
    //
    private final RoundRobinHolder<JedisPool> holder;

    public RedisDirectTransport(Codec codec, int numThreads, ExecutorService es, GenericObjectPoolConfig poolConfig, List<RedisAddress> shards) {
        super(codec, Request.class, numThreads, es);
        holder = HolderFactory.makeRoundRobinHolder();
        for (RedisAddress shard : shards) {
            holder.add(new JedisPool(poolConfig, shard.getHost(), shard.getPort()));
        }
    }

    public RedisDirectTransport(Codec codec, int numThreads, ExecutorService es, RoundRobinHolder<JedisPool> holder) {
        super(codec, Request.class, numThreads, es);
        this.holder = holder;
    }

    @Override
    protected List<? extends MessagePoller> makePollers(Set<String> listenFor, int numThreads) {
        List<RedisMessageConsumer> list = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            list.add(new RedisMessageConsumer(listenFor));
        }

        return list;
    }

    @Override
    public void send(BinaryMessage message) throws Exception {
        LOG.debug("send: {}", message);
        JedisPool pool = holder.next();
        Jedis client = pool.getResource();
        try {
            byte[] key = Utils.asBytes(message.getKey());

            client.lpush(key, message.getPayload());

        } finally {
            pool.returnResource(client);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();

        while (true) {
            try {
                JedisPool pool = holder.next();
                holder.remove(pool);
                pool.destroy();

            } catch (NoSuchElementException ex) {
                break;
            }
        }
    }

    private class RedisMessageConsumer extends MessagePoller {

        private final byte[][] byteKeys;
        private final int blockingTimeout;
        private final JedisPool pool;

        public RedisMessageConsumer(Set<String> topics) {
            byteKeys = new byte[topics.size()][];
            int i = 0;
            for (String s : topics) {
                byteKeys[i++] = Utils.asBytes(s);
            }

            blockingTimeout = (int) TimeUnit.SECONDS.convert(
                    Constants.BLOCKING_TIMEOUT, TimeUnit.MILLISECONDS);

            //each consumer gets a pool to avoid the issue where multiple 
            //consumers are using the same pool while some pools are dormant
            pool = holder.next();
        }

        @Override
        protected Optional<BinaryMessage> nextMessage() {
            //TODO: is there a performance benefit to not using the pool inside the loop?
            Jedis client = pool.getResource();
            try {
                List<byte[]> response = client.brpop(blockingTimeout, byteKeys);
                if (response != null) {
                    String key = Utils.asString(response.get(0));
                    byte[] payload = response.get(1);
                    return Optional.of(new BinaryMessage(key, payload));
                }

            } finally {
                pool.returnResource(client);
            }

            return Optional.absent();
        }
    }
}
