package ldr.server.storage.mem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ldr.client.domen.Embedding;

/**
 * Embeddings in memory (in heap).
 * Class is thread safe.
 */
public class MemoryEmbeddings implements IMemoryEmbeddings {
    private final NavigableMap<Long, Embedding> embeddingsMap = new ConcurrentSkipListMap<>();
    private final AtomicInteger embeddingsByteSize = new AtomicInteger();

    private final int flushThresholdBytes;
    private final int metaEntrySize;

    private final Consumer<List<Embedding>> flushCallback;

    /**
     * @param flushCallback       - callback function which we call after overflowing the threshold.
     *                            We pass the embeddings that should make the overflow in that function.
     */
    public MemoryEmbeddings(Config config, Consumer<List<Embedding>> flushCallback) {
        this.flushThresholdBytes = config.flushThresholdBytes;
        this.metaEntrySize = config.metaEntrySize;
        this.flushCallback = flushCallback;
    }

    @Override
    public void add(Embedding embedding) {
        add(embedding, () -> List.of(embedding));
    }

    @Override
    public void add(List<Embedding> embeddings) {
        for (int i = 0; i < embeddings.size(); i++) {
            // Final for sublist supplier.
            final int current = i;
            add(embeddings.get(i), () -> embeddings.subList(current, embeddings.size()));
        }
    }

    private void add(Embedding embedding, Supplier<List<Embedding>> subList) {
        int size = getEmbeddingsSize(embedding);
        if (embeddingsByteSize.addAndGet(size) > flushThresholdBytes) {
            flushCallback.accept(subList.get());
        } else {
            embeddingsMap.put(embedding.id(), embedding);
        }
    }

    @Override
    public Embedding get(long id) {
        return embeddingsMap.get(id);
    }

    @Override
    public List<Embedding> get(List<Long> ids) {
        List<Embedding> result = new ArrayList<>(ids.size());
        for (long id : ids) {
            Embedding searched = embeddingsMap.get(id);
            if (searched != null) {
                result.add(searched);
            }
        }

        return result;
    }

    @Override
    public Iterator<Embedding> getAll() {
        return embeddingsMap.values().iterator();
    }

    @Override
    public boolean isNeedFlush() {
        return embeddingsByteSize.get() > flushThresholdBytes;
    }

    private int getEmbeddingsSize(Embedding embedding) {
        // 16 - размер заголовкаа объекта (у  нас заголовок Embedding и мапы)
        return Long.BYTES + // Size of long id
                (embedding.vector() == null ? 0 : embedding.vector().length) * Double.BYTES + // Size of vector
                (embedding.metas() == null ? 0 : embedding.metas().size()) * metaEntrySize + // Estimated size of meta
                16 * 2 // header of Embedding and meta
                ;
    }

    /**
     *
     * @param flushThresholdBytes - размер в байтах, после которого надо вызвать flush
     * @param metaEntrySize - приближенный размер entry в мете (используется для подсчета размера embedding-а)
     */
    public record Config(int flushThresholdBytes, int metaEntrySize) {
    }
}
