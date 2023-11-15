package server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import client.IWorkerClient;
import client.WorkerClient;
import ldr.client.domen.Embedding;
import ldr.client.domen.VectorCollectionResult;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"database.location=" + WorkerControllerTest.DB_LOCATION}
)
class WorkerControllerTest {
    static final String DB_LOCATION = "src/test/resources/database";
    private static final Random random = new Random();

    private IWorkerClient workerClient;

    @LocalServerPort
    private int port;

    @BeforeAll
    static void deleting() throws IOException {
        // Удаляю в начале, а не в конце,
        // поскольку спринг закрывает базу после теста и она опять флашится на диск.
        if (Files.exists(Path.of(DB_LOCATION))) {
            FileUtils.deleteDirectory(new File(DB_LOCATION));
        }
    }

    @BeforeEach
    void setUp() {
        workerClient = new WorkerClient("http://localhost:" + port + "/database/collection", "testWorker");
    }

    @Test
    void addAndDeleteCollection() throws IOException {
        String collName = "testColl";

        createCollection(collName, 10);
        assertThat(
                workerClient.deleteCollection(collName).getStatusCode()
        ).isEqualTo(HttpStatus.OK);
    }

    @Test
    void addVectorsTest() throws IOException {
        int vectorLen = 10;
        String coll = "addVectorsTest";
        createCollection(coll, vectorLen);
        var embeddings = generateNearEmbeddings(100, vectorLen, 10);

        for (Embedding emb : embeddings) {
            assertThat(
                    workerClient.addToCollection(emb, coll).getStatusCode()
            ).isEqualTo(HttpStatus.OK);
        }

        ResponseEntity<VectorCollectionResult> vcr = workerClient
                .query(embeddings.get(0).vector(), 10, coll);
        assertThat(vcr.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(vcr.getBody()).isNotNull();
        assertThat(vcr.getBody().results()).isNotNull();
        assertThat(vcr.getBody().results()).isNotEmpty();
    }

    private void createCollection(String name, int vectorLen) {
        assertThat(
                workerClient.createCollection(name, vectorLen).getStatusCode()
        ).isEqualTo(HttpStatus.CREATED);

    }

    // TODO: Make common with ldr-vector-db. To not duplicate code.
    /**
     * Generate near vectors with random id, дельта по каждой координате < coordinateDeltaBound.
     */
    public static List<Embedding> generateNearEmbeddings(int count, int vectorLen, double coordinateDeltaBound) {
        double[] mainVector = generateVector(vectorLen);

        List<Embedding> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(new Embedding(random.nextInt(), generateNearVector(mainVector, coordinateDeltaBound), new HashMap<>()));
        }

        return result;
    }

    private static double[] generateNearVector(double[] mainVector, double coordinateDelta) {
        double[] result = new double[mainVector.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = mainVector[i] + random.nextDouble(coordinateDelta);
        }

        return result;
    }

    public static double[] generateVector(int vectorLen) {
        double[] vector = new double[vectorLen];
        for (int i = 0; i < vectorLen; i++) {
            vector[i] = ThreadLocalRandom.current().nextDouble(1000.0);
        }

        return vector;
    }
}