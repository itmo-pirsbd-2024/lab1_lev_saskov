package jmh.read;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import ldr.client.domen.collection.VectorCollection;

@State(Scope.Benchmark)
public class ReadBenchmark {
    private static final int MAX_NEIGHBOURS_COUNT = 10;

    private volatile VectorCollection collection;
    private volatile double[] vector;

    @Setup
    public void setup() throws IOException {
        collection = VectorCollection.load(new VectorCollection.Config(Prepare.locationCollection));

        // This bound is used for vector from collection
        vector = Prepare.getVector();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 5, time = 10)
    @Measurement(iterations = 5, time = 10)
    public void test(Blackhole bh) {
        bh.consume(collection.query(vector, MAX_NEIGHBOURS_COUNT));
    }

    @TearDown
    public void end() throws IOException {
        collection.close();
    }
}
