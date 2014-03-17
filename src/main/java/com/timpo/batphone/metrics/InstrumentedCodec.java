package com.timpo.batphone.metrics;

import com.codahale.metrics.Timer;
import com.timpo.batphone.codecs.Codec;
import java.io.IOException;

public class InstrumentedCodec implements Codec {

    private final Codec wrappedCodec;
    private final Timer encodePerformance;
    private final Timer decodePerformance;

    public InstrumentedCodec(Codec wrappedCodec) {
        this.wrappedCodec = wrappedCodec;

        String name = wrappedCodec.getClass().getSimpleName();
        encodePerformance = Metrics.get.timer(name + ".encode.performance");
        decodePerformance = Metrics.get.timer(name + ".decode.performance");
    }

    @Override
    public byte[] encode(Object toEncode) throws IOException {
        byte[] encoded;
        Timer.Context encodePerformanceContext = encodePerformance.time();
        try {
            encoded = wrappedCodec.encode(toEncode);

        } finally {
            encodePerformanceContext.stop();
        }
        return encoded;
    }

    @Override
    public <T> T decode(byte[] toDecode, Class<T> decodeAs) throws IOException {
        T decoded;
        Timer.Context decodePerformanceContext = decodePerformance.time();
        try {
            decoded = wrappedCodec.decode(toDecode, decodeAs);

        } finally {
            decodePerformanceContext.stop();
        }
        return decoded;
    }

    @Override
    public String toString() {
        return wrappedCodec.toString();
    }
}
