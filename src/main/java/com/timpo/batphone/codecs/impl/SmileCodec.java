package com.timpo.batphone.codecs.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.timpo.batphone.codecs.Codec;
import java.io.IOException;

public class SmileCodec implements Codec {

    private final ObjectMapper mapper;

    public SmileCodec() {
        this.mapper = new ObjectMapper(new SmileFactory());
    }

    @Override
    public byte[] encode(Object toEncode) throws IOException {
        return mapper.writeValueAsBytes(toEncode);
    }

    @Override
    public <T> T decode(byte[] toDecode, Class<T> decodeAs) throws IOException {
        return mapper.readValue(toDecode, decodeAs);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
