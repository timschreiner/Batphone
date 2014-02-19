package com.timpo.batphone.transports.redis;

import redis.clients.jedis.JedisShardInfo;

public class RedisAddress extends JedisShardInfo {

    public RedisAddress(String host, String name) {
        super(host, name);
    }

    public RedisAddress(String host) {
        super(host);
    }

    public RedisAddress() {
        super("localhost");
    }
}
