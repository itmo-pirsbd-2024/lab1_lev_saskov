package ldr.client.domen;

import java.util.List;

/**
 * @param results - distanced embeddings that sorted from small distance to big (small distance goes first).
 */
public record VectorCollectionResult(List<DistancedEmbedding> results) {
    public static final VectorCollectionResult EMPTY = new VectorCollectionResult(null);

    public boolean isEmpty() {
        return results == null || results.isEmpty();
    }
}
