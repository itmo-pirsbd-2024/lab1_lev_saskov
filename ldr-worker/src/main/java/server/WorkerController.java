package server;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ldr.client.domen.Embedding;
import ldr.client.domen.VectorCollectionResult;
import ldr.client.domen.db.IDataBase;

@RestController
public class WorkerController {
    private final Logger log = LoggerFactory.getLogger(WorkerController.class);
    private final IDataBase dataBase;

    public WorkerController(IDataBase dataBase) {
        this.dataBase = dataBase;
    }

    @PostMapping("/database/collection")
    @ResponseStatus(code = HttpStatus.CREATED, reason = "Collection was created")
    void createCollection(@RequestParam String name, @RequestParam int vectorLen) throws IOException {
        log.info("createCollection with name {}, vectorLen {}", name, vectorLen);

        try {
            dataBase.createCollection(name, vectorLen);
        } catch (IOException exception) {
            log.error("IOException during creating collection. ", exception);
            throw exception;
        }
    }

    @DeleteMapping("/database/collection")
    @ResponseStatus(code = HttpStatus.OK, reason = "Collection was deleted")
    void deleteCollection(@RequestParam String name) throws IOException {
        log.info("deleteCollection with name {}", name);
        dataBase.removeCollection(name);
    }

    @PutMapping("/database/collection")
    @ResponseStatus(code = HttpStatus.OK, reason = "Collection was renamed")
    void renameCollection(@RequestParam String oldName, @RequestParam String newName) {
        log.info("renameCollection {} -> {}", oldName, newName);
        dataBase.renameCollection(oldName, newName);
    }

    //    Example:
    //    curl -X PUT 'localhost:8080/database/collection/collTest' -H 'Content-Type: application/json' -d '{"id":10, "vector":[5.0, 320.3, 32.4]}'
    @PutMapping(value = "/database/collection/{collectionName}")
    @ResponseStatus(code = HttpStatus.OK, reason = "Vector was added")
    void addToCollection(@RequestBody Embedding embedding, @PathVariable String collectionName) {
        // TODO: add list.
        log.info("addToCollection {} embedding: {}", collectionName, embedding);
        dataBase.getCollection(collectionName).add(embedding);
    }

    @DeleteMapping("/database/collection/{collectionName}")
    @ResponseStatus(code = HttpStatus.OK, reason = "Vector was deleted")
    void deleteFromCollection(@RequestParam long id, @PathVariable String collectionName) {
        // TODO: add list.
        log.info("deleteFromCollection {} embedding: {}", collectionName, id);
        dataBase.getCollection(collectionName).delete(id);
    }

    // Example
    //    curl -X GET 'localhost:8080/database/collection/collTest?vector=10.0,11.0&maxNeighboursCount=10'
    @GetMapping(value = "/database/collection/{collectionName}")
    ResponseEntity<VectorCollectionResult> query(@RequestParam double[] vector,
                         @RequestParam int maxNeighboursCount,
                         @PathVariable String collectionName) {
        log.info("query. Vector: {}, vectorLen: {}, collectionName: {}", vector, maxNeighboursCount, collectionName);
        var body = dataBase.getCollection(collectionName).query(vector, maxNeighboursCount);
        return new ResponseEntity<>(body, HttpStatus.OK);
    }
}
