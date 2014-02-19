package com.timpo.batphone.metrics;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.timpo.batphone.codecs.Compressor;
import java.io.IOException;

public class InstrumentedCompressor implements Compressor {

    private final  Compressor wrappedCompressor;
    private final Timer compressPerformance;
    private final Timer decompressPerformance;
    private final Histogram compressionSavings;
    private final Histogram compressionRatio;

    public InstrumentedCompressor(Compressor wrappedCompressor) {
        this.wrappedCompressor = wrappedCompressor;

        String name = wrappedCompressor.getClass().getSimpleName();
        compressPerformance = Metrics.get.timer(name + ".compress.performance");
        decompressPerformance = Metrics.get.timer(name + ".decompress.performance");

        compressionSavings = Metrics.get.histogram(name + ".compress.savings");
        compressionRatio = Metrics.get.histogram(name + ".compress.ratio");
    }

    @Override
    public byte[] compress(byte[] toCompress) throws IOException {
        byte[] compressed;
        Timer.Context compressPerformanceContext = compressPerformance.time();
        try {
            compressed = wrappedCompressor.compress(toCompress);

        } finally {
            compressPerformanceContext.stop();
        }

        //track the benefits of using compression
        compressionSavings.update(toCompress.length - compressed.length);
        compressionRatio.update((int) (1.0 * compressed.length / toCompress.length * 100));

        return compressed;
    }

    @Override
    public byte[] decompress(byte[] toDecompress) throws IOException {
        byte[] decompressed;
        Timer.Context decompressPerformanceContext = decompressPerformance.time();
        try {
            decompressed = wrappedCompressor.decompress(toDecompress);

        } finally {
            decompressPerformanceContext.stop();
        }
        return decompressed;
    }
}
