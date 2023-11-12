package server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import org.springframework.http.ResponseEntity;
@RestController
public class Controller {
    private final Logger log = LoggerFactory.getLogger(Controller.class);

    // curl -X POST -d "name=Colection1&vectorLen=5" http://localhost:8080/database/collection
    @PostMapping("/database/collection")
    public String createCollection(@RequestParam String name, @RequestParam int vectorLen) throws IOException {
        log.info("createCollection with name {}, vectorLen {}", name, vectorLen);
        String result = "Received data: Name = " + name + ", Vector Length = " + vectorLen;
        return result;
    }

    @DeleteMapping("/database/collection")
    public String deleteCollection(@RequestParam String name) throws IOException {
        log.info("deleteCollection with name {}", name);
        String result = "Received data: Name = " + name;
        return result;
    }

    @PutMapping("/database/collection")
    public String renameCollection(@RequestParam String oldName, @RequestParam String newName) {
        log.info("renameCollection {} -> {}", oldName, newName);
        String result = "Received data: oldName = " + oldName + ", newName = " + newName;
        return result;
    }

    @PutMapping(value = "/database/collection/{collectionName}")
    public String addToCollection(@RequestBody Embedding embedding, @PathVariable String collectionName) {
        //log.info("addToCollection {} embedding: {}", collectionName, embedding);
        String result = "Received data: Embedding = " + embedding + ", Name = " + collectionName;
        return result;
    }

    @DeleteMapping("/database/collection/{collectionName}")
    public String deleteFromCollection(@RequestParam long id, @PathVariable String collectionName) {
        String result = "Received data: id = " + id + ", Name = " + collectionName;
        return result;
    }

    @GetMapping("/database/collection/{collectionName}")
    public String query(@RequestParam double[] vector, @RequestParam int maxNeighboursCount, @PathVariable String collectionName) {
        log.info("query. Vector: {}, vectorLen: {}, collectionName: {}", vector, maxNeighboursCount, collectionName);
        return "  ";
    }





}
