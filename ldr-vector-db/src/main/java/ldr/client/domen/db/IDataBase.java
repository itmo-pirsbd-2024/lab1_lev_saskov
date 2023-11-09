package ldr.client.domen.db;

import java.io.IOException;

import ldr.client.domen.collection.IVectorCollection;

public interface IDataBase {
    /**
     * Throws NoSuchElementException, if not exists.
     */
    IVectorCollection getCollection(String name);

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
