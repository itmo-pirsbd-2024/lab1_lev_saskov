package jmh.read;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import jmh.TestUtils;
import ldr.client.domen.Embedding;
import ldr.client.domen.collection.VectorCollection;
import ldr.server.serialization.my.VectorEncoder;

public class Prepare {
    private static final int COUNT = 100_000;
    private static final int META_SIZE = 10;
    private static final int DIM = 100;
    private static final VectorEncoder vectorEncoder = new VectorEncoder();

    public static final Path locationCollection = Paths.get(
        "jmh", "data", String.format("collection_c_%d_d_%d_m_%d", COUNT, DIM, META_SIZE)
    );

    public static final Path locationVector = Paths.get(
        "jmh", "data", String.format("vector_%d", DIM)
    );

    public static double[] getVector() throws IOException {
        return vectorEncoder.decode(Files.readAllBytes(locationVector)).result();
    }

    public static void main(String[] args) throws IOException {
        List<Embedding> embeddings = TestUtils.generateFixedEmbeddings(COUNT, DIM, META_SIZE);
        var collection = VectorCollection.load(new VectorCollection.Config(locationCollection, DIM));
        collection.add(embeddings);
        collection.close();

        var vector = TestUtils.generateVector(DIM, 1.0);
        Files.write(locationVector, vectorEncoder.encode(vector));
    }
}
