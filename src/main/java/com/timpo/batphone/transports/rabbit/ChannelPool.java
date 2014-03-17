package com.timpo.batphone.transports.rabbit;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.timpo.batphone.other.Utils;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import org.slf4j.Logger;

public class ChannelPool extends GenericObjectPool<Channel> {

    private static final Logger LOG = Utils.logFor(ChannelPool.class);

    public ChannelPool(Connection conn, int maxChannels) {
        super(new ChannelPoolFactory(conn));

        setTestWhileIdle(true);
        setMinEvictableIdleTimeMillis(1000 * 10);
        setMaxTotal(maxChannels);
    }

    public ChannelPool(Connection conn) {
        this(conn, 16);
    }

    static class ChannelPoolFactory extends BasePooledObjectFactory<Channel> {

        private final Connection conn;

        public ChannelPoolFactory(Connection conn) {
            this.conn = conn;
        }

        @Override
        public Channel create() throws Exception {
            LOG.debug("create");
            return conn.createChannel();
        }

        @Override
        public PooledObject<Channel> wrap(Channel chan) {
            return new DefaultPooledObject<>(chan);
        }

        @Override
        public boolean validateObject(PooledObject<Channel> chan) {
            LOG.debug("validateObject");
            return chan.getObject().isOpen();
        }

        @Override
        public void destroyObject(PooledObject<Channel> chan) throws Exception {
            LOG.debug("destroyObject");
            chan.getObject().close();
        }
    }
}