package ldr.server.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import ldr.client.domen.Embedding;

import static ldr.server.TestUtils.generateManyEmbeddings;
import static ldr.server.TestUtils.getRandomSubList;
import static ldr.server.TestUtils.resourcesPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageManagerTest {
    private static final Path storagePath = resourcesPath.resolve("storage");
    private static final int metaEntrySize = 15;

    @Test
    public void testAddAndGet() throws IOException {
        testAddAndGet(100_000, "testAddAndGet");
    }

    @Test
    public void testWithFlush() throws IOException {
        testAddAndGet(5000, "testWithFlush");
    }

    @Test
    public void testConcurrent() throws IOException, InterruptedException {
        testConcurrent(100_000, "testConcurrent");
    }

    @Test
    public void testConcurrentWithFlush() throws IOException, InterruptedException {
        testConcurrent(5000, "testConcurrentWithFlush");
    }

    @Test
    public void testDelete() throws IOException {
        Path location = Files.createTempDirectory(storagePath, "testDelete");
        IStorageManager storage = StorageManager.load(new StorageManager.Config(location, 5000, metaEntrySize));
        var added = testAddAndGet(storage);
        // testAddAndGet creates 100 embeddings, 20 of them we will delete.
        List<Long> toRemove = getRandomSubList(20, added).stream().map(Embedding::id).toList();
        storage.remove(toRemove);
        toRemove.forEach(id -> assertNull(storage.get(id)));
        FileUtils.deleteDirectory(location.toFile());
    }

    private void testConcurrent(int flushThresholdBytes, String testId) throws IOException, InterruptedException {
        Path location = Files.createTempDirectory(storagePath, testId);
        IStorageManager storage = StorageManager.load(new StorageManager.Config(location, flushThresholdBytes, metaEntrySize));

        int nThreads = 5;
        ExecutorService executors = Executors.newFixedThreadPool(nThreads);
        for (int i = 0; i < nThreads; i++) {
            executors.execute(() -> testAddAndGet(storage));
        }

        executors.shutdown();
        boolean done = executors.awaitTermination(5000, TimeUnit.MILLISECONDS);
        assertTrue(done, "Timeout in test concurrent");
        FileUtils.deleteDirectory(location.toFile());
    }

    public void testAddAndGet(int flushThresholdBytes, String testId) throws IOException {
        Path location = Files.createTempDirectory(storagePath, testId);
        IStorageManager storage = StorageManager.load(new StorageManager.Config(location, flushThresholdBytes, metaEntrySize));
        testAddAndGet(storage);
        FileUtils.deleteDirectory(location.toFile());
    }

    private List<Embedding> testAddAndGet(IStorageManager storage) {
        var embeddings = generateManyEmbeddings();
        storage.add(embeddings);
        var result = storage.get(embeddings.stream().map(Embedding::id).toList());
        assertEquals(embeddings.size(), result.size());
        for (int i = 0; i < embeddings.size(); i++) {
            assertEquals(embeddings.get(i), result.get(i));
        }
        embeddings.forEach(e -> assertEquals(e, storage.get(e.id())));
        return embeddings;
    }
}