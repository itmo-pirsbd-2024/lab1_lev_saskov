package ldr.server.storage.drive;

import java.io.IOException;
import java.util.Iterator;

import ldr.client.domen.Embedding;
import ldr.server.storage.IEmbeddingGetter;

public interface IHardDriveEmbeddings extends IEmbeddingGetter {
    void save(Iterator<Embedding> embeddingIterator) throws IOException;
}
