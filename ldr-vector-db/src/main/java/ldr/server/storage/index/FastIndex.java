package ldr.server.storage.index;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.debatty.java.lsh.LSHSuperBit;
import ldr.client.domen.Embedding;

public class FastIndex implements IFastIndex {
    private static final Logger log = LoggerFactory.getLogger(FastIndex.class);
    private static final int INITIAL_SEED = 42;
    private static final int STAGES = 5;
    private static final int BUCKETS = 15;

    private final ObjectMapper mapper = new ObjectMapper();
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final LSHSuperBit lsh;
    private final Path location;

    private final List<Int2ObjectMap<LongSet>> stageBuckets;
    private volatile boolean closed;

    public static FastIndex load(Config config) {
        return new FastIndex(config, loadFromFile(config.filePath));
    }

    // Use load.
    private FastIndex(Config config, List<Int2ObjectMap<LongSet>> stageBuckets) {
        this.location = config.filePath();
        this.stageBuckets = stageBuckets;
        this.lsh = new LSHSuperBit(STAGES, BUCKETS, config.vectorLen(), INITIAL_SEED);
    }

    @Override
    public Set<Long> getNearest(double[] vector) {
        checkClosed();
        try {
            readWriteLock.readLock().lock();

            int[] buckets = getBuckets(vector);
            Set<Long> nearest = new HashSet<>(stageBuckets.get(0).get(buckets[0]));
            for (int i = 0; i < buckets.length; i++) {
                var stage = stageBuckets.get(i);
                nearest.retainAll(stage.get(buckets[i]));
            }

            return nearest;
        } finally {
            readWriteLock.readLock().unlock();
        }

    }

    @Override
    public void add(Embedding embedding) {
        checkClosed();
        withWriteLock(() -> addUnsafe(embedding));
    }

    @Override
    public void add(List<Embedding> embeddings) {
        checkClosed();
        withWriteLock(() -> embeddings.forEach(this::addUnsafe));
    }

    private void addUnsafe(Embedding embedding) {
        int[] buckets = getBuckets(embedding.vector());
        for (int stageN = 0; stageN < buckets.length; stageN++) {
            var stage = stageBuckets.get(stageN);
            int bucket = buckets[stageN];
            var ids = stage.get(buckets[stageN]);
            if (ids == null) {
                ids = new LongOpenHashSet();
            }
            ids.add(embedding.id());
            stage.putIfAbsent(bucket, ids);
        }
    }

    @Override
    public void remove(long id) {
        checkClosed();
        withWriteLock(() -> removeUnsafe(id));
    }

    @Override
    public void remove(Collection<Long> ids) {
        checkClosed();
        withWriteLock(() -> ids.forEach(this::removeUnsafe));
    }

    private void removeUnsafe(long id) {
        for (var stage : stageBuckets) {
            for (var bucket : stage.values()) {
                bucket.remove(id);
            }
        }
    }

    @Override
    public void flush() {
        withWriteLock(() -> {
            try {
                mapper.writeValue(location.toFile(), stageBuckets);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void close() throws IOException {
        checkClosed();
        closed = true;
        flush();
    }

    private void withWriteLock(Runnable runnable) {
        try {
            readWriteLock.writeLock().lock();
            runnable.run();
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private void checkClosed() {
        if (closed) {
            throw new RuntimeException("Fast index already closed.");
        }
    }

    private int[] getBuckets(double[] vector) {
        return lsh.hash(vector);
    }

    private static List<Int2ObjectMap<LongSet>> loadFromFile(Path location) {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<Integer, Set<Long>>> proxy = null;
        if (Files.exists(location)) {
            try {
                proxy = mapper.readValue(location.toFile(), new TypeReference<>() {
                });
                log.info("Index found. Initialization from file.");
            } catch (IOException e) {
                log.error("Can't read index file, it will be created.", e);
            }
        }

        List<Int2ObjectMap<LongSet>> index = new ArrayList<>();
        if (proxy != null) {
            for (var m : proxy) {
                var stage = new Int2ObjectArrayMap<LongSet>(m.size());
                m.forEach((key, value) -> stage.put((int) key, new LongOpenHashSet(value)));
                index.add(stage);
            }
        } else {
            for (int i = 0; i < STAGES; i++) {
                index.add(new Int2ObjectArrayMap<>());
            }
        }

        return index;
    }

    /**
     * @param filePath - path of file, where index will be written. Will be written after close, if not presented.
     */
    public record Config(Path filePath, int vectorLen) {
    }
}
