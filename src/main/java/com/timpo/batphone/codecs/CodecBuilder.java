package com.timpo.batphone.codecs;

import com.timpo.batphone.metrics.InstrumentedCodec;
import com.timpo.batphone.metrics.InstrumentedCompressor;

public class CodecBuilder {

    private Codec codec = null;
    private Compressor compressor = null;
    private boolean instrumentCompressor = false;
    private boolean instrumentCodec = false;

    public Codec build() {
        if (codec == null) {
            throw new IllegalArgumentException("codec cannot be null");
        }

        if (compressor != null) {
            if (instrumentCompressor) {
                compressor = new InstrumentedCompressor(compressor);
            }

            codec = new CompressedCodec(codec, compressor);
        }

        if (instrumentCodec) {
            codec = new InstrumentedCodec(codec);
        }

        return codec;
    }

    private CodecBuilder(Codec codec) {
        this.codec = codec;
    }

    public static CodecBuilder setCodec(Codec codec) {
        return new CodecBuilder(codec);
    }

    public CodecBuilder setCompressor(Compressor compressor) {
        this.compressor = compressor;
        return this;
    }

    public CodecBuilder instrumentCodec() {
        instrumentCodec = true;
        return this;
    }

    public CodecBuilder instrumentCompressor() {
        instrumentCompressor = true;
        return this;
    }

    public CodecBuilder instrument() {
        instrumentCodec = true;
        instrumentCompressor = true;
        return this;
    }
}
