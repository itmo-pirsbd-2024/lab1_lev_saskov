package ldr.server.storage;

import java.util.Collection;

public interface IEmbeddingRemover {
    void remove(long id);
    void remove(Collection<Long> ids);
}
