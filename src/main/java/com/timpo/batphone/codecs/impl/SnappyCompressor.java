package com.timpo.batphone.codecs.impl;

import com.timpo.batphone.codecs.Compressor;
import java.io.IOException;
import org.xerial.snappy.Snappy;

public class SnappyCompressor implements Compressor {

    @Override
    public byte[] compress(byte[] toCompress) throws IOException {
        return Snappy.compress(toCompress);
    }

    @Override
    public byte[] decompress(byte[] toDecompress) throws IOException {
        return Snappy.uncompress(toDecompress);
    }
    
    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
