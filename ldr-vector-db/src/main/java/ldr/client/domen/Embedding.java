package ldr.client.domen;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

// TODO: Поддержать null metas. В том числе и в кодерах.
public record Embedding(long id, @Nonnull double[] vector, @Nonnull Map<String, String> metas) {
    /**
     * The default hashCode in record is bad for double[], because it use object hashCode.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, Arrays.hashCode(vector), metas);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof Embedding e) {
            if (e.id != id || e.vector.length != vector.length || e.metas.size() != metas.size()) {
                return false;
            }

            for (int i = 0; i < vector.length; i++) {
                if (e.vector[i] != vector[i]) {
                    return false;
                }
            }

            return metas.equals(e.metas);
        } else {
            return false;  // other is not Embedding
        }
    }

    @Override
    public String toString() {
        return String.format("Embedding[id=%d, vector=%s, metas=%s]", id, Arrays.toString(vector), metas);
    }
}
