package ldr.client.domen.db;

import java.io.IOException;

import ldr.client.domen.collection.IVectorCollection;

public interface IDataBase {
    IVectorCollection getCollection(String name);

    void createCollection(String name, int vectorLen) throws IOException;

    void removeCollection(String name) throws IOException;

    void renameCollection(String oldName, String newName);

    void close() throws IOException;
}
