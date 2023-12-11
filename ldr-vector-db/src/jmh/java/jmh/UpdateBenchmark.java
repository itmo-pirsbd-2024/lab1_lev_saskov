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
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ldr.client.domen.Embedding;
import ldr.client.domen.collection.VectorCollection;
import ldr.server.TestUtils;

@State(Scope.Benchmark)
public class UpdateBenchmark {
    public static final Path resourcesPath = Paths.get("src", "jmh", "resources", "bench", "update");
    private static final Logger log = LoggerFactory.getLogger(CreateBenchmark.class);

    //    @Param({"20", "50", "100", "200", "300"})
//    private int DIM;
    private static final int DIM = 100;

    //    @Param({"20000", "50000", "100000", "200000", "300000"})
//    private int COUNT;
    private static final int COUNT = 100_000;

    @Param({"2", "5", "10", "20", "30"})
    private int META_SIZE;
//    private static final int META_SIZE = 10;

    private List<Embedding> embeddings;
    private VectorCollection collection;
    private Path location;

    @Setup(Level.Invocation)
    public void setup() throws IOException {
        embeddings = TestUtils.generateFixedEmbeddings(COUNT, DIM, META_SIZE);
        location = Files.createTempDirectory(resourcesPath, "collection");

        collection = VectorCollection.load(new VectorCollection.Config(location, DIM));
        collection.add(embeddings);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @BenchmarkMode(Mode.AverageTime)
    @Fork(value = 1, warmups = 1)
    @Warmup(iterations = 1, time = 1)
    @Measurement(iterations = 1, time = 1)
    public void test() {
        collection.add(embeddings);
    }

    @TearDown(Level.Invocation)
    public void end() throws IOException {
        collection.close();
        log.info("Result Size bench. dim:{} count:{} meta_size:{}, collection size: {}", DIM, COUNT, META_SIZE,
                FileUtils.sizeOfDirectory(location.toFile())
        );
        FileUtils.deleteDirectory(location.toFile());
    }
}
