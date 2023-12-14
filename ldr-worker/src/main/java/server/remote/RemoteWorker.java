package server.remote;

import java.util.Iterator;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ldr.client.domen.Embedding;
import ldr.client.domen.VectorCollectionResult;
import ldr.client.domen.collection.IVectorCollection;

// TODO: avoid hardcode with paramnames.
public class RemoteWorker extends Worker implements IRemoteWorker {
    private final RestTemplate restTemplate = new RestTemplate();

    public RemoteWorker(String name, String host) {
        super(name, host);
    }

    /**
     * У remote worker-a internal реализация, поскольку к нему может обращаться только другой воркер.
     */
    @Override
    public ResponseEntity<String> addToCollection(Embedding embedding, String collectionName) {
        return perform(HttpMethod.PUT, collectionToPath(collectionName), new HttpEntity<>(embedding));
    }

    @Override
    public ResponseEntity<String> deleteFromCollection(long id, String collectionName) {
        return perform(HttpMethod.DELETE, collectionToPath(collectionName), null, new Param("id", id));
    }

    // TODO: Мб подрефакторить ldr-vector-db и добавить имя в VectorCollection
    @Override
    public ResponseEntity<String> sendCollection(String collectionName, IVectorCollection collection) {
        Iterator<Embedding> all = collection.getAll();
        // TODO: Отсылать чанками, а не по одному.
        while (all.hasNext()) {
            addToCollection(all.next(), collectionName);
        }

        return new ResponseEntity<>(HttpStatus.OK);
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

    private String collectionToPath(String collectionName) {
        return "/internal/" + collectionName;
    }

    private ResponseEntity<String> perform(HttpMethod method, String path, HttpEntity<?> requestBody, Param... params) {
        return perform(method, path, requestBody, String.class, params);
    }

    private <T> ResponseEntity<T> perform(HttpMethod method, String path, HttpEntity<?> requestBody,
                                          Class<T> responseType, Param... params) {
        StringBuilder uri = new StringBuilder(host).append(path);
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
