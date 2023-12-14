package ldr.client.domen.db;

import java.io.IOException;
import java.util.List;

import ldr.client.domen.collection.IVectorCollection;

public interface IDataBase {
    /**
     * Throws NoSuchElementException, if not exists.
     */
    IVectorCollection getCollection(String name);

    List<String> getAllCollections();

    /**
     * Throws KeyAlreadyExistsException, if already presented.
     */
    void createCollection(String name, int vectorLen) throws IOException;

    /**
     * Throws NoSuchElementException, if not exists.
     */
    void removeCollection(String name) throws IOException;

    /**
     * Throws NoSuchElementException, if not exists.
     */
    void renameCollection(String oldName, String newName);

    void close() throws IOException;
}
