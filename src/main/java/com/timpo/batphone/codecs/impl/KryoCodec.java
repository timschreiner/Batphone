package com.timpo.batphone.codecs.impl;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.timpo.batphone.codecs.Codec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class KryoCodec implements Codec {

    private final Kryo kryo;

    public KryoCodec() {
        kryo = new Kryo();
    }

    @Override
    public byte[] encode(Object toEncode) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try (Output output = new Output(stream)) {
            kryo.writeObject(output, toEncode);
        }

        return stream.toByteArray(); // Serialization done, get bytes
    }

    @Override
    public <T> T decode(byte[] toDecode, Class<T> decodeAs) throws IOException {
        Input input = new Input(new ByteArrayInputStream(toDecode), toDecode.length);
        
        return kryo.readObject(input, decodeAs);
    }
    
    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
