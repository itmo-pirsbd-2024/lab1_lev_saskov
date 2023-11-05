package ldr.server.storage;

import java.io.Closeable;
import java.io.Flushable;

public interface IHardMemory extends IEmbeddingKeeper, IEmbeddingRemover, Flushable, Closeable {
}
