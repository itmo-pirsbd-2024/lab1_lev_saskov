package server;

import ldr.client.domen.Embedding;
import ldr.client.domen.VectorCollectionResult;

public interface IWorkerPool {
    // TODO: Return type with status and consistency flags.
    void createCollection(String name, int vectorLen);
    void deleteCollection(String name);
    void renameCollection(String oldName, String newName);
    void addToCollection(Embedding embedding, String collectionName);
    void deleteFromCollection(long id, String collectionName);
    VectorCollectionResult query(double[] vector, int maxNeighboursCount, String collectionName);
}
