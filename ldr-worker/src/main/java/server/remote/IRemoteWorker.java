package server.remote;

import org.springframework.http.ResponseEntity;

import ldr.client.domen.collection.IVectorCollection;

public interface IRemoteWorker extends IWorker {
    ResponseEntity<String> sendCollection(String collectionName, IVectorCollection collection);
}
