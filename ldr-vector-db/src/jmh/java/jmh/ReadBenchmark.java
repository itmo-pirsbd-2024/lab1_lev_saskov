package jmh;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import ldr.client.domen.Embedding;
import ldr.client.domen.collection.VectorCollection;
import ldr.server.TestUtils;

import static ldr.server.TestUtils.generateVector;

@State(Scope.Benchmark)
public class ReadBenchmark {
    public static final Path resourcesPath = Paths.get("src", "jmh", "resources", "bench", "read");
    private static final int MAX_NEIGHBOURS_COUNT = 10;

    @Param({"20", "50", "100", "200", "300"})
    private int DIM;
//    private static final int DIM = 100;

//    @Param({"20000", "50000", "100000", "200000", "300000"})
//    private int COUNT;
    private static final int COUNT = 100_000;

    private static final int META_SIZE = 10;

    private VectorCollection collection;
    private Path location;
    private double[] randVector;

    @Setup
    public void setup() throws IOException {
        List<Embedding> embeddings = TestUtils.generateFixedEmbeddings(COUNT, DIM, META_SIZE);
        location = Files.createTempDirectory(resourcesPath, "collection");

        collection = VectorCollection.load(new VectorCollection.Config(location, DIM));
        collection.add(embeddings);
        // This bound is used for vector from collection
        randVector = generateVector(DIM, 1.0);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @BenchmarkMode(Mode.AverageTime)
    @Fork(value = 1, warmups = 1)
    @Warmup(iterations = 3, time = 1)
    @Measurement(iterations = 5, time = 1)
    public void test(Blackhole bh) {
        bh.consume(collection.query(randVector, MAX_NEIGHBOURS_COUNT));
    }

    @TearDown
    public void end() throws IOException {
        collection.close();
        FileUtils.deleteDirectory(location.toFile());
    }
}
