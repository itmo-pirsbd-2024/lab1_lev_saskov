package client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ldr.client.domen.Embedding;
import ldr.client.domen.VectorCollectionResult;

// TODO: avoid hardcode with paramnames.
public class WorkerClient implements IWorkerClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String dbUrl;
    // Can be used for identification and rendezvous hashing
    private final String nodeName;

    public WorkerClient(String dbUrl, String nodeName) {
        this.dbUrl = dbUrl;
        this.nodeName = nodeName;
    }

    @Override
    public ResponseEntity<String> createCollection(String name, int vectorLen) {
        return perform(HttpMethod.POST, new Param("name", name), new Param("vectorLen", vectorLen));
    }

    @Override
    public ResponseEntity<String> deleteCollection(String name) {
        return perform(HttpMethod.DELETE, new Param("name", name));
    }

    // TODO: test it.
    @Override
    public ResponseEntity<String> renameCollection(String oldName, String newName) {
        return perform(HttpMethod.PUT, new Param("oldName", oldName), new Param("newName", newName));
    }

    @Override
    public ResponseEntity<String> addToCollection(Embedding embedding, String collectionName) {
        return perform(HttpMethod.PUT, collectionToPath(collectionName), new HttpEntity<>(embedding));
    }

    // TODO: test it.
    @Override
    public ResponseEntity<String> deleteFromCollection(long id, String collectionName) {
        return perform(HttpMethod.DELETE, new Param("id", id), new Param("collectionName", collectionName));
    }

    @Override
    public ResponseEntity<VectorCollectionResult> query(double[] vector, int maxNeighboursCount, String collectionName) {
        StringBuilder vectorStr = new StringBuilder();
        for (double val : vector) {
            vectorStr.append(val).append(",");
        }
        vectorStr.deleteCharAt(vectorStr.length() - 1);

        return perform(
                HttpMethod.GET,
                collectionToPath(collectionName),
                null, VectorCollectionResult.class,
                new Param("vector", vectorStr.toString()),
                new Param("maxNeighboursCount", 10)
        );
    }

    @Override
    public String getNodeName() {
        return nodeName;
    }

    private String collectionToPath(String collectionName) {
        return "/" + collectionName;
    }

    private ResponseEntity<String> perform(HttpMethod method, Param... params) {
        return perform(method, "", null, String.class, params);
    }

    private ResponseEntity<String> perform(HttpMethod method, String path, HttpEntity<?> requestBody, Param... params) {
        return perform(method, path, requestBody, String.class, params);
    }

    private <T> ResponseEntity<T> perform(HttpMethod method, String path, HttpEntity<?> requestBody,
                                          Class<T> responseType, Param... params) {
        StringBuilder uri = new StringBuilder(dbUrl).append(path);
        if (params.length != 0) {
            uri.append("?");
            final int lastParam = params.length - 1;
            for (int i = 0; i < lastParam; i++) {
                uri.append(params[i]).append("&");
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
}
