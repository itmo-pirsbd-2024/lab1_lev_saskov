package ldr.client.domen.collection;

import java.io.Closeable;
import java.util.Collection;
import java.util.List;

import ldr.client.domen.Embedding;
import ldr.client.domen.VectorCollectionResult;

public interface IVectorCollection extends Closeable {
    /**
     * If embedding is already presented, then it will be updated.
     */
    void add(Embedding embedding);

    void add(List<Embedding> embeddings);

    VectorCollectionResult query(double[] vector, int maxNeighborsCount);

    /**
     * Own rules for filter, for ex meta1Key:eq:meta1Val
     */
    VectorCollectionResult query(double[] vector, int maxNeighborsCount, String filter);

    void delete(long id);

    void delete(Collection<Long> ids);

    void delete(String filter);
}
