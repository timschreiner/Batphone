package com.timpo.batphone.codecs;

import java.io.IOException;

/**
 * Compresses and decompresses byte arrays
 */
public interface Compressor {

    /**
     * Decompress a raw byte array
     *
     * @param toCompress
     * @return compressed bytes
     * @throws IOException
     */
    public byte[] compress(byte[] toCompress) throws IOException;

    /**
     * Decompress a compressed byte array
     *
     * @param toDecompress
     * @return decompressed bytes
     * @throws IOException
     */
    public byte[] decompress(byte[] toDecompress) throws IOException;
}
