package server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void addAndDeleteCollection() throws IOException {
        Param collName = new Param("name", "testColl");

        assertThat(
                perform(HttpMethod.POST, collName, new Param("vectorLen", 10)).getStatusCode()
        ).isEqualTo(HttpStatus.CREATED);

        assertThat(
                perform(HttpMethod.DELETE, collName).getStatusCode()
        ).isEqualTo(HttpStatus.OK);

        FileUtils.deleteDirectory(new File(DB_LOCATION));
    }

    @Test
    void addVectorsTest() throws IOException {
        int vectorLen = 10;
        String coll = "addVectorsTest";
        createCollection("addVectorsTest", vectorLen);
        var embeddings = generateNearEmbeddings(100, vectorLen, 10);

        String path = "/" + coll;
        for (Embedding emb : embeddings) {
            assertThat(
                    perform(HttpMethod.PUT, path, new HttpEntity<>(emb)).getStatusCode()
            ).isEqualTo(HttpStatus.OK);
        }

        StringBuilder vector = new StringBuilder();
        for (double val : embeddings.get(0).vector()) {
            vector.append(val).append(",");
        }
        vector.deleteCharAt(vector.length() - 1);

        ResponseEntity<VectorCollectionResult> vcr = perform(
                HttpMethod.GET, path, null, VectorCollectionResult.class,
                new Param("vector", vector.toString()),
                new Param("maxNeighboursCount", 10)
        );
        assertThat(vcr.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(vcr.getBody()).isNotNull();
        assertThat(vcr.getBody().results()).isNotNull();
        assertThat(vcr.getBody().results()).isNotEmpty();

        FileUtils.deleteDirectory(new File(DB_LOCATION));
    }

    private void createCollection(String name, int vectorLen) {
        Param nameParam = new Param("name", name);
        Param vectorLenParam = new Param("vectorLen", vectorLen);

        assertThat(
                perform(HttpMethod.POST, nameParam, vectorLenParam).getStatusCode()
        ).isEqualTo(HttpStatus.CREATED);

    }

    private ResponseEntity<String> perform(HttpMethod method, Param... params) {
        return perform(method, "", null, String.class, params);
    }

    private ResponseEntity<String> perform(HttpMethod method, String path, HttpEntity<?> requestBody, Param... params) {
        return perform(method, path, requestBody, String.class, params);
    }

    private <T> ResponseEntity<T> perform(HttpMethod method, String path, HttpEntity<?> requestBody,
                                          Class<T> responseType, Param... params) {
        StringBuilder uri = new StringBuilder("http://localhost:" + port + "/database/collection" + path);
        if (params.length != 0) {
            uri.append("?");
            final int lastParam = params.length - 1;
            for (int i = 0; i < lastParam; i++) {
                uri.append(params[i]);
                uri.append("&");
            }
            uri.append(params[lastParam]);
        }
        return restTemplate.exchange(uri.toString(), method, requestBody, responseType);
    }

    record Param(String name, Object value) {
        @Override
        public String toString() {
            return name + "=" + value;
        }
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

    private static double[] generateVector(int vectorLen) {
        double[] vector = new double[vectorLen];
        for (int i = 0; i < vectorLen; i++) {
            vector[i] = ThreadLocalRandom.current().nextDouble(1000.0);
        }

        return vector;
    }
}