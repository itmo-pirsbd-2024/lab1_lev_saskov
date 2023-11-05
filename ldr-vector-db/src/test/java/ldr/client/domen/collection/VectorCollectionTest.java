package ldr.client.domen.collection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import ldr.client.domen.DistancedEmbedding;
import ldr.client.domen.Embedding;
import ldr.client.domen.VectorCollectionResult;

import static ldr.server.TestUtils.generateNearEmbeddings;
import static ldr.server.TestUtils.randomInt;
import static ldr.server.TestUtils.resourcesPath;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VectorCollectionTest {
    private static final Path indexFolder = resourcesPath.resolve("collection");

    @Test
    public void testAddAndSearch() throws IOException {
        Path location = Files.createTempDirectory(indexFolder, "testAddAndSearch");

        int vectorLen = 10;
        IVectorCollection collection = VectorCollection.load(new VectorCollection.Config(location, vectorLen));
        var groups = addGroups(collection, 10, vectorLen);

        checkSearch(collection, groups);

        FileUtils.deleteDirectory(location.toFile());
    }

    @Test
    public void testAddAndSearchWithClose() throws IOException {
        Path location = Files.createTempDirectory(indexFolder, "testAddAndSearchWithClose");
        int vectorLen = 10;
        List<List<Embedding>> groups;
        try(IVectorCollection collection = VectorCollection.load(new VectorCollection.Config(location, vectorLen))) {
            groups = addGroups(collection, 10, vectorLen);
            checkSearch(collection, groups);
        }

        try(IVectorCollection collection = VectorCollection.load(new VectorCollection.Config(location, vectorLen))) {
            checkSearch(collection, groups);
        }

        FileUtils.deleteDirectory(location.toFile());
    }

    private List<List<Embedding>> addGroups(IVectorCollection collection, int groupsCount, int vectorLen) {
        List<List<Embedding>> groups = new ArrayList<>(groupsCount);
        for (int i = 0; i < groupsCount; i++) {
            var group = generateNearEmbeddings(1000, vectorLen, 10);
            groups.add(group);
            collection.add(group);
        }

        return groups;
    }

    private void checkSearch(IVectorCollection collection, List<List<Embedding>> groups) {
        int correctGroups = 0;
        for (var group : groups) {
            double[] randVectorFromGroup = group.get(randomInt(group.size())).vector();
            VectorCollectionResult result = collection.query(randVectorFromGroup, 100);
            List<DistancedEmbedding> results = result.results();
            assertFalse(result.isEmpty());
            checkOrder(results);

            // Проверяем, что все результаты из близкой группы
            var resultsIds = results.stream().map(r -> r.embedding().id()).collect(Collectors.toSet());
            var groupIds = group.stream().map(Embedding::id).collect(Collectors.toSet());
            resultsIds.retainAll(groupIds);
            if (results.size() == resultsIds.size()) {
                correctGroups++;
            }
        }

        assertTrue(correctGroups > groups.size() / 1.5);
    }

    private void checkOrder(List<DistancedEmbedding> results) {
        var prevRes = results.get(0);
        for (int i = 1; i < results.size(); i++) {
            assertTrue(prevRes.distance() <= results.get(i).distance());
        }
    }
}