package com.timpo.batphone.transports;

import java.util.Objects;

public class NetworkAddress {

    private final int port;
    private final String host;

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

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + this.port;
        hash = 83 * hash + Objects.hashCode(this.host);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NetworkAddress other = (NetworkAddress) obj;
        if (this.port != other.port) {
            return false;
        }
        if (!Objects.equals(this.host, other.host)) {
            return false;
        }
        return true;
    }
    //</editor-fold>
}
