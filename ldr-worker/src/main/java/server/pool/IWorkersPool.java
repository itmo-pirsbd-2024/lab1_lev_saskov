package server.pool;

import org.springframework.http.ResponseEntity;

import ldr.client.domen.Embedding;
import ldr.client.domen.VectorCollectionResult;

public interface IWorkersPool {
    ResponseEntity<String> createCollection(String name, int vectorLen);

    ResponseEntity<String> deleteCollection(String name);

    // TODO: Сложно в реализации, да и смысла в них нет
//    void renameCollection(String oldName, String newName);
//    void renameCollectionInternal(String oldName, String newName);

    ResponseEntity<String> addToCollection(Embedding embedding, String collectionName);

    /**
     * Запрос пришел от другого воркера,
     * к коллекции уже приклеен домен и такая коллекция уже должна быть на локальном воркере.
     *
     * @param collectionName - с приклеенным доменом
     */
    ResponseEntity<String> addToCollectionInternal(Embedding embedding, String collectionName);

    ResponseEntity<String> deleteFromCollection(long id, String collectionName);

    ResponseEntity<String> deleteFromCollectionInternal(long id, String collectionName);

    ResponseEntity<VectorCollectionResult> query(double[] vector, int maxNeighboursCount, String collectionName);

    ResponseEntity<VectorCollectionResult> queryInternal(double[] vector, int maxNeighboursCount, String collectionName);
}
