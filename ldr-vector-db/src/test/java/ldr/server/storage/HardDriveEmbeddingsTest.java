package ldr.server.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ldr.client.domen.Embedding;
import ldr.server.storage.drive.HardDriveEmbeddings;
import ldr.server.storage.drive.IHardDriveEmbeddings;

import static ldr.server.TestUtils.generateManyEmbeddings;
import static ldr.server.TestUtils.getRandomSubList;
import static ldr.server.TestUtils.resourcesPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HardDriveEmbeddingsTest {
    private static final Path drivePath = resourcesPath.resolve("drive");

    @Test
    public void testLoading() throws IOException {
        testDriveEmbeddings("testLoading", driveEmbeddings -> {
            var embeddings = uploadEmbeddings(driveEmbeddings);
            checkEmbeddings(driveEmbeddings, embeddings);
        });
    }

    @Test
    public void testManyLoading() throws IOException {
        testDriveEmbeddings("testManyLoading", driveEmbeddings -> {
            List<List<Embedding>> embLists = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                embLists.add(uploadEmbeddings(driveEmbeddings));
            }

            for (var embeddings : embLists) {
               checkEmbeddings(driveEmbeddings, embeddings);
            }
        });
    }

    @Test
    public void testPriority() throws IOException {
        // Если есть пересечения по id, то должен сохраняться только свежепришедший.
        testDriveEmbeddings("testPriority", driveEmbeddings -> {
            var oldEmb = uploadNotRandom(driveEmbeddings);
            var newEmb = uploadNotRandom(driveEmbeddings);

            checkEmbeddings(driveEmbeddings, newEmb);
            for (var embedding : oldEmb) {
                if (ArrayUtils.isNotEmpty(embedding.vector())) {
                    // Empty vectors could be equal, it is correct.
                    Assertions.assertNotEquals(embedding, driveEmbeddings.get(embedding.id()));
                }
            }
        });
    }

    /**
     * We shouldn't save deleted items. Grave is flag for deleted.
     */
    @Test
    public void testGraves() throws IOException {
        testDriveEmbeddings("testGraves", driveEmbeddings -> {
            var uploaded = uploadEmbeddings(driveEmbeddings);
            // uploadEmbeddings creates 100 embeddings, 20 of them we will delete.
            List<Embedding> toRemove = getRandomSubList(20, uploaded);
            List<Embedding> graves = toRemove.stream()
                    .map(e -> StorageManager.createGrave(e.id())).toList();
            driveEmbeddings.save(graves.iterator());

            uploaded.removeAll(toRemove);
            checkEmbeddings(driveEmbeddings, uploaded);

            for (Embedding deleted : graves) {
                assertNull(driveEmbeddings.get(deleted.id()));
            }
        });
    }

    private void testDriveEmbeddings(String testId, WithDriveEmbeddings test) throws IOException {
        Path location = Files.createTempDirectory(drivePath, testId);
        IHardDriveEmbeddings driveEmbeddings = HardDriveEmbeddings.load(new HardDriveEmbeddings.Config(location));

        test.apply(driveEmbeddings);

        FileUtils.deleteDirectory(location.toFile());
    }

    private List<Embedding> uploadEmbeddings(IHardDriveEmbeddings driveEmbeddings) throws IOException {
        var embeddings = generateManyEmbeddings();
        embeddings.sort(Comparator.comparingLong(Embedding::id));
        driveEmbeddings.save(embeddings.iterator());
        return embeddings;
    }

    private List<Embedding> uploadNotRandom(IHardDriveEmbeddings driveEmbeddings) throws IOException {
        var embeddings = generateManyEmbeddings(100, 20, 10, true);
        driveEmbeddings.save(embeddings.iterator());
        return embeddings;
    }

    private void checkEmbeddings(IHardDriveEmbeddings driveEmbeddings, List<Embedding> embeddings) {
        for (Embedding embedding : embeddings) {
            assertEquals(embedding, driveEmbeddings.get(embedding.id()));
        }

        List<Long> ids = embeddings.stream().map(Embedding::id).toList();
        List<Embedding> actual = driveEmbeddings.get(ids);
        assertEquals(embeddings.size(), actual.size());
        assertEquals(embeddings, actual);
    }

    @FunctionalInterface
    interface WithDriveEmbeddings {
        void apply(IHardDriveEmbeddings driveEmbeddings) throws IOException;
    }
}