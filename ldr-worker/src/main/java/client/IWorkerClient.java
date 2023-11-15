package client;

import org.springframework.http.ResponseEntity;

import ldr.client.domen.Embedding;
import ldr.client.domen.VectorCollectionResult;

public interface IWorkerClient {
    ResponseEntity<String> createCollection(String name, int vectorLen);
    ResponseEntity<String> deleteCollection(String name);
    ResponseEntity<String> renameCollection(String oldName, String newName);
    ResponseEntity<String> addToCollection(Embedding embedding, String collectionName);
    ResponseEntity<String> deleteFromCollection(long id, String collectionName);
    ResponseEntity<VectorCollectionResult> query(double[] vector, int maxNeighboursCount, String collectionName);
    String getNodeName();
}
