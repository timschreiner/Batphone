package com.timpo.batphone.transports;

import com.timpo.batphone.other.Utils;

public class BinaryMessage {

    private String key;
    private final byte[] payload;

    @Override
    public String toString() {
        return "BinaryMessage{" + "key=" + key + ", payload=" + payloadAsString() + '}';
    }

    public String payloadAsString() {
        if (payload.length < 2048) {
            return Utils.asString(payload);
        } else {
            return payload.length + " bytes";
        }
    }

    //<editor-fold defaultstate="collapsed" desc="generated-code">
    public BinaryMessage(String key, byte[] payload) {
        this.key = key;
        this.payload = payload;
    }

    public String getKey() {
        return key;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setKey(String key) {
        this.key = key;
    }
    //</editor-fold>
}
