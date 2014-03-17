package com.timpo.batphone.codecs.impl;

import com.timpo.batphone.codecs.Codec;
import com.timpo.batphone.other.Utils;
import java.io.IOException;

public class JSONCodec implements Codec {

    @Override
    public byte[] encode(Object toEncode) throws IOException {
        return Utils.asBytes(Utils.JSON.writeValueAsString(toEncode));
    }

    @Override
    public <T> T decode(byte[] toDecode, Class<T> decodeAs) throws IOException {
        return Utils.JSON.readValue(Utils.asString(toDecode), decodeAs);
    }
    
    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
