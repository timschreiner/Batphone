package com.timpo.batphone.transports.rabbit;

import com.timpo.batphone.transports.NetworkAddress;

public class RabbitAddress extends NetworkAddress {

    public RabbitAddress(String host) {
        super(host, 5672);
    }

    public RabbitAddress() {
        super(5672);
    }
}
