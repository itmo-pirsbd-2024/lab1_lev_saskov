package server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ldr.client.domen.Embedding;
import ldr.client.domen.VectorCollectionResult;
import server.pool.IWorkersPool;

@RestController
public class WorkerController {
    private final Logger log = LoggerFactory.getLogger(WorkerController.class);
    private final IWorkersPool workersPool;

    public WorkerController(IWorkersPool workersPool) {
        this.workersPool = workersPool;
    }

    @PostMapping("/database/collection")
    ResponseEntity<String> createCollection(@RequestParam String name, @RequestParam int vectorLen) {
        log.info("createCollection with name {}, vectorLen {}", name, vectorLen);
        return workersPool.createCollection(name, vectorLen);
    }

    @DeleteMapping("/database/collection")
    ResponseEntity<String> deleteCollection(@RequestParam String name) {
        log.info("deleteCollection with name {}", name);
        return workersPool.deleteCollection(name);
    }

    //    Example:
    //    curl -X PUT 'localhost:8080/database/collection/collTest' -H 'Content-Type: application/json' -d '{"id":10, "vector":[5.0, 320.3, 32.4]}'
    @PutMapping(value = "/database/collection/{collectionName}")
    ResponseEntity<String> addToCollection(@RequestBody Embedding embedding, @PathVariable String collectionName) {
        // TODO: add list.
        log.info("addToCollection {} embedding: {}", collectionName, embedding);
        return workersPool.addToCollection(embedding, collectionName);
    }

    @PutMapping(value = "/database/collection/internal/{collectionName}")
    ResponseEntity<String> addToCollectionInternal(@RequestBody Embedding embedding, @PathVariable String collectionName) {
        // TODO: add list.
        log.info("addToCollectionInternal {} embedding: {}", collectionName, embedding);
        return workersPool.addToCollectionInternal(embedding, collectionName);
    }

    @DeleteMapping("/database/collection/{collectionName}")
    ResponseEntity<String> deleteFromCollection(@RequestParam long id, @PathVariable String collectionName) {
        // TODO: add list.
        log.info("deleteFromCollection {} embedding: {}", collectionName, id);
        return workersPool.deleteFromCollection(id, collectionName);
    }

    @DeleteMapping("/database/collection/internal/{collectionName}")
    ResponseEntity<String> deleteFromCollectionInternal(@RequestParam long id, @PathVariable String collectionName) {
        // TODO: add list.
        log.info("deleteFromCollection {} embedding: {}", collectionName, id);
        return workersPool.deleteFromCollectionInternal(id, collectionName);
    }

    // Example
    //    curl -X GET 'localhost:8080/database/collection/collTest?vector=10.0,11.0&maxNeighboursCount=10'
    @GetMapping(value = "/database/collection/{collectionName}")
    ResponseEntity<VectorCollectionResult> query(@RequestParam double[] vector,
                                                 @RequestParam int maxNeighboursCount,
                                                 @PathVariable String collectionName) {
        log.info("query. Vector: {}, vectorLen: {}, collectionName: {}", vector, maxNeighboursCount, collectionName);
        return workersPool.query(vector, maxNeighboursCount, collectionName);
    }

    @GetMapping(value = "/database/collection/internal/{collectionName}")
    ResponseEntity<VectorCollectionResult> queryInternal(@RequestParam double[] vector,
                                                         @RequestParam int maxNeighboursCount,
                                                         @PathVariable String collectionName) {
        log.info("queryInternal. Vector: {}, vectorLen: {}, collectionName: {}", vector, maxNeighboursCount, collectionName);
        return workersPool.queryInternal(vector, maxNeighboursCount, collectionName);
    }
}
