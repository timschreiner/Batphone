package com.timpo.batphone.transports.rabbit;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.timpo.batphone.other.Utils;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;

public class ChannelPool extends GenericObjectPool<Channel> {

    private static final Logger LOG = Utils.logFor(ChannelPool.class);

    public ChannelPool(Connection conn) {
        super(new ChannelPoolFactory(conn));

        setTestWhileIdle(true);
        setMinEvictableIdleTimeMillis(1000 * 10);
        setMaxActive(16);
    }

    static class ChannelPoolFactory extends BasePoolableObjectFactory<Channel> {

        private final Connection conn;

        public ChannelPoolFactory(Connection conn) {
            this.conn = conn;
        }

        @Override
        public Channel makeObject() throws Exception {
            LOG.debug("makeObject");
            return conn.createChannel();
        }

        @Override
        public void destroyObject(Channel chan) throws Exception {
            LOG.debug("destroyObject");
            chan.close();
        }

        @Override
        public boolean validateObject(Channel chan) {
            LOG.debug("validateObject");
            return chan.isOpen();
        }
    }
}