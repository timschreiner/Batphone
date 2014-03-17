package com.timpo.batphone.codecs;

import java.io.IOException;

public class CompressedCodec implements Codec {

    private Codec wrappedCodec;
    private Compressor wrappedCompressor;

    public CompressedCodec(Codec wrappedCodec, Compressor wrappedCompressor) {
        this.wrappedCodec = wrappedCodec;
        this.wrappedCompressor = wrappedCompressor;
    }

    @Override
    public byte[] encode(Object toEncode) throws IOException {
        return wrappedCompressor.compress(wrappedCodec.encode(toEncode));
    }

    @Override
    public <T> T decode(byte[] toDecode, Class<T> decodeAs) throws IOException {
        return wrappedCodec.decode(wrappedCompressor.decompress(toDecode), decodeAs);
    }

    @Override
    public String toString() {
        return wrappedCodec.toString();
    }
}
