package ldr.server.storage;

import java.util.Iterator;
import java.util.List;

import ldr.client.domen.Embedding;

public interface IEmbeddingGetter {
    /**
     * @return Embedding with given id, if it isn't presented returns null.
     */
    Embedding get(long id);

    /**
     * List of all found embeddings with given ids.
     */
    List<Embedding> get(List<Long> ids);

    /**
     * flush then get all from disc, if more writing during reading will be done iterator won't be updated.
     */
    Iterator<Embedding> getAll();
}
