package ldr.server.storage;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ldr.client.domen.Embedding;
import ldr.server.storage.mem.IMemoryEmbeddings;
import ldr.server.storage.mem.MemoryEmbeddings;

import static ldr.server.TestUtils.generateManyEmbeddings;
import static ldr.server.TestUtils.getRandomSubList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryEmbeddingsTest {
    private static List<Embedding> commonEmbeddings;

    @BeforeAll
    public static void setup() {
        commonEmbeddings = generateManyEmbeddings(100, 20, 10);
    }

    @Test
    public void testAddAngGetOneByOne() {
        // No flush in this test, so null for callback.
        IMemoryEmbeddings memoryEmbeddings = new MemoryEmbeddings(
                new MemoryEmbeddings.Config(100_000, 10), null);
        commonEmbeddings.forEach(memoryEmbeddings::add);
        for (Embedding embedding : commonEmbeddings) {
            Embedding fromMem = memoryEmbeddings.get(embedding.id());
            assertEquals(embedding, fromMem);
        }
    }

    @Test
    public void testAddManyAngGetMany() {
        IMemoryEmbeddings memoryEmbeddings = new MemoryEmbeddings(
                new MemoryEmbeddings.Config(100_000, 10), null);
        memoryEmbeddings.add(commonEmbeddings);
        List<Long> ids = commonEmbeddings.stream().map(Embedding::id).toList();
        List<Embedding> actual = memoryEmbeddings.get(ids);
        assertEquals(commonEmbeddings.size(), actual.size());
        assertEquals(commonEmbeddings, actual);
    }

    @Test
    public void testFlushCallback() {
        List<Embedding> toFlush = new ArrayList<>();
        // 100 - flush threshold bites. Small count, so we expect flush callback on some step.
        IMemoryEmbeddings memoryEmbeddings = new MemoryEmbeddings(
                new MemoryEmbeddings.Config(100, 10), toFlush::addAll);
        memoryEmbeddings.add(commonEmbeddings);

        assertTrue(memoryEmbeddings.isNeedFlush());
        assertFalse(toFlush.isEmpty());
    }

    /**
     * Grave is a flag for delete, but we should return it and keep in memory to know about deleting.
     * Final deleting will be after saving on drive (graves won't be saved).
     */
    @Test
    public void testGravesInMemory() {
        IEmbeddingKeeper memoryEmbeddings = new MemoryEmbeddings(
                new MemoryEmbeddings.Config(100_000, 10), null);
        memoryEmbeddings.add(commonEmbeddings);
        var toRemove = getRandomSubList(20, commonEmbeddings);
        var graves =  toRemove.stream().map(e -> StorageManager.createGrave(e.id())).toList();
        memoryEmbeddings.add(graves);

        graves.forEach(e -> assertTrue(StorageManager.isGrave(e)));
    }
}