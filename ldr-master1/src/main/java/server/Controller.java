package server;

import ldr.client.domen.Embedding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Map;

@RestController
public class Controller {
    private final Logger log = LoggerFactory.getLogger(Controller.class);

    // curl -X POST -d "name=Colection1&vectorLen=5" http://localhost:8080/database/collection
    @PostMapping("/database/collection")
    public Map<String, String> createCollection(@RequestParam String name, @RequestParam int vectorLen) throws IOException {
        log.info("createCollection with name {}, vectorLen {}", name, vectorLen);
        Map<String, String> result = Map.of("name", name,"vectorLen", Integer.toString(vectorLen));
        return result;
    }

    @DeleteMapping("/database/collection")
    public Map<String, String> deleteCollection(@RequestParam String name) throws IOException {
        log.info("deleteCollection with name {}", name);
        Map<String, String> result = Map.of("name", name);
        return result;
    }

    @PutMapping("/database/collection")
    public Map<String, String> renameCollection(@RequestParam String oldName, @RequestParam String newName) {
        log.info("renameCollection {} -> {}", oldName, newName);
        Map<String, String> result = Map.of("oldName", oldName, "newName", newName);
        return result;
    }

    @PutMapping(value = "/database/collection/{collectionName}")
    public Map<String, String> addToCollection(@RequestBody Embedding embedding, @PathVariable String collectionName) {
        log.info("addToCollection {} embedding: {}", collectionName, embedding);
        System.out.println("Received PUT request for collection: " + collectionName);
        System.out.println("Embedding ID: " + embedding.id());
        System.out.println("Embedding Values: " + embedding.vector());
        System.out.println("Embedding Metadata: " + embedding.metas());

        Map<String, String> result = Map.of("embedding", embedding.toString(), "collectionName", collectionName);
        return result;
    }

    @DeleteMapping("/database/collection/{collectionName}")
    public Map<String, String> deleteFromCollection(@RequestParam long id, @PathVariable String collectionName) {
        Map<String, String> result = Map.of("id", Long.toString(id), "collectionName", collectionName);
        return result;
    }

    @GetMapping("/database/collection/{collectionName}")
    public String query(@RequestParam double[] vector, @RequestParam int maxNeighboursCount, @PathVariable String collectionName) {
        log.info("query. Vector: {}, vectorLen: {}, collectionName: {}", vector, maxNeighboursCount, collectionName);
        return "  ";
    }





}
