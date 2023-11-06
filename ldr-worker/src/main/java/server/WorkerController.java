package server;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ldr.client.domen.Embedding;
import ldr.client.domen.db.IDataBase;

@RestController
public class WorkerController {
    private final Logger log = LoggerFactory.getLogger(WorkerController.class);
    private final IDataBase dataBase;

    public WorkerController(IDataBase dataBase) {
        this.dataBase = dataBase;
    }

    @PostMapping("/database/collection")
    void createCollection(@RequestParam String name, @RequestParam int vectorLen) {
        log.info("createCollection with name {}, vectorLen {}", name, vectorLen);
    }

    @DeleteMapping("/database/collection")
    void deleteCollection(@RequestParam String name, @RequestParam int vectorLen) {
        log.info("deleteCollection with name {}, vectorLen {}", name, vectorLen);
    }

    @PutMapping("/database/collection")
    void renameCollection(@RequestParam String oldName, @RequestParam String newName) {
        log.info("renameCollection {} -> {}", oldName, newName);
    }

    //    Example:
    //    curl -X PUT 'localhost:8080/database/collection/collTest' -H 'Content-Type: application/json' -d '{"id":10, "vector":[5.0, 320.3, 32.4]}'
    @PutMapping(value = "/database/collection/{collectionName}")
    void addToCollection(@RequestBody Embedding embedding, @PathVariable String collectionName) {
        // TODO: add list.
        log.info("addToCollection {} embedding: {}", collectionName, embedding);
    }

    @DeleteMapping("/database/collection/{collectionName}")
    void deleteFromCollection(@RequestParam long id, @PathVariable String collectionName) {
        // TODO: add list.
        log.info("deleteFromCollection {} embedding: {}", collectionName, id);
    }

    // Example
    //    curl -X GET 'localhost:8080/database/collection/collTest?vector=10.0,11.0&maxNeighboursCount=10'
    @GetMapping("/database/collection/{collectionName}")
    void query(@RequestParam List<Double> vector, @RequestParam int maxNeighboursCount, @PathVariable String collectionName) {
        //TODO: test with array
        log.info("query. Vector: {}, vectorLen: {}, collectionName: {}", vector, maxNeighboursCount, collectionName);
    }
}
