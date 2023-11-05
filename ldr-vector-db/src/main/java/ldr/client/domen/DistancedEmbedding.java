package ldr.client.domen;

public record DistancedEmbedding(Embedding embedding, double distance) implements Comparable<DistancedEmbedding> {
    @Override
    public int compareTo(DistancedEmbedding o) {
        return Double.compare(this.distance, o.distance);
    }
}
