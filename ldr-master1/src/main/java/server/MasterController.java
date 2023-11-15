package server;

import ldr.client.domen.Embedding;
import ldr.client.domen.VectorCollectionResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
public class MasterController {
    private final Logger log = LoggerFactory.getLogger(MasterController.class);
    private final IWorkerPool workerPool;

    public MasterController(IWorkerPool workerPool) {
        this.workerPool = workerPool;
    }

    // curl -X POST -d "name=Colection1&vectorLen=5" http://localhost:8080/database/collection
    @PostMapping("/database/collection")
    @ResponseStatus(code = HttpStatus.CREATED, reason = "Collection was created")
    void createCollection(@RequestParam String name, @RequestParam int vectorLen) throws IOException {
        log.info("Master createCollection with name {}, vectorLen {}", name, vectorLen);
        workerPool.createCollection(name, vectorLen);
    }

    @DeleteMapping("/database/collection")
    @ResponseStatus(code = HttpStatus.OK, reason = "Collection was deleted")
    void deleteCollection(@RequestParam String name) throws IOException {
        log.info("Master deleteCollection with name {}", name);
        workerPool.deleteCollection(name);
    }

    @PutMapping("/database/collection")
    @ResponseStatus(code = HttpStatus.OK, reason = "Collection was renamed")
    void renameCollection(@RequestParam String oldName, @RequestParam String newName) {
        log.info("Master renameCollection {} -> {}", oldName, newName);
        workerPool.renameCollection(oldName, newName);
    }

    //    Example:
    //    curl -X PUT 'localhost:8080/database/collection/collTest' -H 'Content-Type: application/json' -d '{"id":10, "vector":[5.0, 320.3, 32.4]}'
    @PutMapping(value = "/database/collection/{collectionName}")
    @ResponseStatus(code = HttpStatus.OK, reason = "Vector was added")
    void addToCollection(@RequestBody Embedding embedding, @PathVariable String collectionName) {
        // TODO: add list.
        log.info("Master addToCollection {} embedding: {}", collectionName, embedding);
        workerPool.addToCollection(embedding, collectionName);
    }

    @DeleteMapping("/database/collection/{collectionName}")
    @ResponseStatus(code = HttpStatus.OK, reason = "Vector was deleted")
    void deleteFromCollection(@RequestParam long id, @PathVariable String collectionName) {
        // TODO: add list.
        log.info("Master deleteFromCollection {} embedding: {}", collectionName, id);
        workerPool.deleteFromCollection(id, collectionName);
    }

    // Example
    //    curl -X GET 'localhost:8080/database/collection/collTest?vector=10.0,11.0&maxNeighboursCount=10'
    @GetMapping(value = "/database/collection/{collectionName}")
    ResponseEntity<VectorCollectionResult> query(@RequestParam double[] vector,
                                                 @RequestParam int maxNeighboursCount,
                                                 @PathVariable String collectionName) {
        log.info("Master query. Vector: {}, vectorLen: {}, collectionName: {}", vector, maxNeighboursCount, collectionName);
        var body = workerPool.query(vector, maxNeighboursCount, collectionName);
        return new ResponseEntity<>(body, HttpStatus.OK);
    }
}
