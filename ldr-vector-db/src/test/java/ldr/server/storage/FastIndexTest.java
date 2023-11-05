package ldr.server.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ldr.client.domen.Embedding;
import ldr.server.storage.index.FastIndex;
import ldr.server.storage.index.IFastIndex;

import static ldr.server.TestUtils.generateNearEmbeddings;
import static ldr.server.TestUtils.randomInt;
import static ldr.server.TestUtils.resourcesPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class FastIndexTest {
    private static final Path indexFolder = resourcesPath.resolve("index");

    @Test
    public void testNearest() throws IOException {
        Path indexFile = Files.createTempFile(indexFolder, "testWithCloseSimple", null);

        final int vectorLen = 10;
        // 5 бакетов на 10 групп.
        // Used without closing, because we don't check saving in this test.
        IFastIndex fastIndex = FastIndex.load(new FastIndex.Config(indexFile, vectorLen));
        List<List<Embedding>> groups = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            var group = generateNearEmbeddings(1000, vectorLen, 10);
            groups.add(group);
            fastIndex.add(group);
        }

        // Проверяем FastIndex правильно отдает группы близких векторов.
        int correctGroups = 0;
        for (var group : groups) {
            double[] randVectorFromGroup = group.get(randomInt(group.size())).vector();
            Set<Long> nearIds = fastIndex.getNearest(randVectorFromGroup);
            var groupIds = group.stream().map(Embedding::id).collect(Collectors.toSet());
            groupIds.retainAll(nearIds);
            // Проверяем что большая часть векторов из группы попала в корзину правильно.
            if (groupIds.size() > group.size() / 1.5) {
                correctGroups++;
            }
        }

        // Проверяем, что большая часть групп распределена корректно.
        assertTrue(correctGroups > groups.size() / 1.5);

        Files.delete(indexFile);
    }

    @Test
    public void testWithCloseSimple() throws IOException {
        int vectorLen = 10;
        Path indexFile = Files.createTempFile(indexFolder, "testWithCloseSimple", null);
        IFastIndex fastIndex = FastIndex.load(new FastIndex.Config(indexFile, vectorLen));

        var embeddings = generateNearEmbeddings(100, vectorLen, 0.3);
        double[] oneOfVector = embeddings.get(6).vector();

        fastIndex.add(embeddings);
        var nearOneOf = fastIndex.getNearest(oneOfVector);
        assertFalse(nearOneOf.isEmpty());

        fastIndex.close();
        fastIndex = FastIndex.load(new FastIndex.Config(indexFile, vectorLen));
        var nearOneOfAfterClose = fastIndex.getNearest(oneOfVector);

        assertEquals(nearOneOf, nearOneOfAfterClose);
        Files.delete(indexFile);
    }

    @Test
    public void testWithClose() throws IOException {
        int vectorLen = 10;
        Path indexFile = Files.createTempFile(indexFolder, "testWithClose", null);
        IFastIndex fastIndex = FastIndex.load(new FastIndex.Config(indexFile, vectorLen));

        int groupsCount = 10;
        List<double[]> oneOfList = new ArrayList<>(groupsCount);
        for (int i = 0; i < groupsCount; i++) {
            var embeddings = generateNearEmbeddings(100, vectorLen, 0.3);
            fastIndex.add(embeddings);
            oneOfList.add(embeddings.get(6).vector());
        }

        Map<double[], Set<Long>> nearMap = new HashMap<>(oneOfList.size());
        for (double[] oneOf : oneOfList) {
            var nearOneOf = fastIndex.getNearest(oneOf);
            assertFalse(nearOneOf.isEmpty());
            nearMap.put(oneOf, nearOneOf);
        }

        fastIndex.close();
        fastIndex = FastIndex.load(new FastIndex.Config(indexFile, vectorLen));

        for (var near : nearMap.entrySet()) {
            var nearAfterClose = fastIndex.getNearest(near.getKey());
            assertEquals(near.getValue(), nearAfterClose);
        }

        Files.delete(indexFile);
    }
}