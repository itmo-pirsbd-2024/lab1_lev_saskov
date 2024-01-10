package server.remote;

import org.springframework.http.ResponseEntity;

import ldr.client.domen.Embedding;
import ldr.client.domen.VectorCollectionResult;

public interface IWorker {
    String getName();
    String getHost();

//    ResponseEntity<String> renameCollection(String oldName, String newName);
    ResponseEntity<String> addToCollection(Embedding embedding, String collectionName);
    ResponseEntity<String> deleteFromCollection(long id, String collectionName);
    ResponseEntity<VectorCollectionResult> query(double[] vector, int maxNeighboursCount, String collectionName);
}
