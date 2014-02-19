package com.timpo.batphone.transports.kafka;

import com.timpo.batphone.transports.NetworkAddress;

public class BrokerAddress extends NetworkAddress {

    public BrokerAddress(String host) {
        super(host, 9092);
    }

    public BrokerAddress() {
        super(9092);
    }
}
