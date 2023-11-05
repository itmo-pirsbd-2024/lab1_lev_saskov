package ldr.server.storage.mem;

import java.util.Iterator;

import ldr.client.domen.Embedding;
import ldr.server.storage.IEmbeddingGetter;
import ldr.server.storage.IEmbeddingKeeper;

public interface IMemoryEmbeddings extends IEmbeddingKeeper, IEmbeddingGetter {
    Iterator<Embedding> getAll();

    boolean isNeedFlush();
}
