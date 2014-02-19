package com.timpo.batphone.transports;

public class NetworkAddress {

    private final String host;
    private final int port;

    @Override
    public String toString() {
        return host + ":" + port;
    }

    public NetworkAddress(int port) {
        this.port = port;

        host = "localhost";
    }

    //<editor-fold defaultstate="collapsed" desc="comment">
    public NetworkAddress(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
    //</editor-fold>
}
