package com.timpo.batphone.transports.kafka;

import com.timpo.batphone.transports.NetworkAddress;

public class ZookeeperAddress extends NetworkAddress {

    public ZookeeperAddress(String host) {
        super(host, 2181);
    }

    public ZookeeperAddress() {
        super(2181);
    }
}
