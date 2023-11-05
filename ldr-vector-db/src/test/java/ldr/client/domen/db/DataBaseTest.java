package ldr.client.domen.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import ldr.client.domen.collection.IVectorCollection;

import static ldr.server.TestUtils.generateNearEmbeddings;
import static ldr.server.TestUtils.randomInt;
import static ldr.server.TestUtils.resourcesPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataBaseTest {
    private static final Path databasePath = resourcesPath.resolve("database");
    private static final String COLLECTION_NAME_PREFIX = "coll";
    private static final int START_VECTOR_LEN = 10;

    @Test
    public void testCreateCollection() throws IOException {
        Path location = Files.createTempDirectory(databasePath, "testCreateCollection");
        createAndTestCollection(location, COLLECTION_NAME_PREFIX, 10);
        FileUtils.deleteDirectory(location.toFile());
    }

    @Test
    public void testCreateManyCollections() throws IOException {
        Path location = Files.createTempDirectory(databasePath, "testCreateManyCollections");
        for (int i = 0; i < 10; i++) {
            createAndTestCollection(location, COLLECTION_NAME_PREFIX + i, START_VECTOR_LEN + i);
        }

        FileUtils.deleteDirectory(location.toFile());
    }

    @Test
    public void testRemoveCollection() throws IOException {
        Path location = Files.createTempDirectory(databasePath, "testRemoveCollection");
        String[] collectionNames = createCollections(location);
        String[] toRemoveArr = getThreeFrom(collectionNames);

        final IDataBase db = DataBase.load(location);
        for (String toRemove : toRemoveArr) {
            db.removeCollection(toRemove);
            assertThrows(NoSuchElementException.class, () -> db.getCollection(toRemove));
        }

        db.close();
        final IDataBase dbAfterClose = DataBase.load(location);
        for (String toRemove : toRemoveArr) {
            assertThrows(NoSuchElementException.class, () -> dbAfterClose.getCollection(toRemove));
        }

        FileUtils.deleteDirectory(location.toFile());
    }

    @Test
    public void testRenameCollection() throws IOException {
        Path location = Files.createTempDirectory(databasePath, "testRenameCollection");
        String[] collectionNames = createCollections(location);
        String[] toRenameArr = getThreeFrom(collectionNames);
        String[] newNames = Arrays.stream(toRenameArr).map(n -> "new" + n).toList().toArray(new String[0]);
        final IDataBase db = DataBase.load(location);
        for (int i = 0; i < toRenameArr.length; i++) {
            final String toRename = toRenameArr[i];
            final String newName = newNames[i];

            db.renameCollection(toRename, newName);
            assertThrows(NoSuchElementException.class, () -> db.getCollection(toRename));
            // Just check that collection presented with new name.
            assertNotNull(db.getCollection(newName));
        }
        db.close();

        final IDataBase dbAfterClose = DataBase.load(location);
        for (int i = 0; i < toRenameArr.length; i++) {
            final String toRename = toRenameArr[i];
            assertThrows(NoSuchElementException.class, () -> dbAfterClose.getCollection(toRename));
            // Just check that collection presented with new name.
            assertNotNull(dbAfterClose.getCollection(newNames[i]));
        }

        FileUtils.deleteDirectory(location.toFile());
    }

    private String[] getThreeFrom(String[] arr) {
        return new String[]{arr[0], arr[arr.length / 2], arr[arr.length - 1]};
    }

    private String[] createCollections(Path location) throws IOException {
        int collectionCount = 10;
        String[] collectionNames = new String[collectionCount];
        for (int i = 0; i < collectionCount; i++) {
            String collName = COLLECTION_NAME_PREFIX + i;
            createAndTestCollection(location, collName, START_VECTOR_LEN + i);
            collectionNames[i] = collName;
        }

        return collectionNames;
    }

    private void createAndTestCollection(Path location, String collName, int vectorLen) throws IOException {
        IDataBase db = DataBase.load(location);
        db.createCollection(collName, vectorLen);
        IVectorCollection collection = db.getCollection(collName);
        var embeddings = generateNearEmbeddings(100, vectorLen, 10);
        collection.add(embeddings);
        double[] rand = embeddings.get(randomInt(embeddings.size())).vector();
        int maxResCount = 10;
        var res = collection.query(rand, maxResCount);
        assertFalse(res.isEmpty());
        db.close();

        db = DataBase.load(location);
        collection = db.getCollection(collName);
        assertEquals(collection.query(rand, maxResCount), res);
        db.close();
    }
}