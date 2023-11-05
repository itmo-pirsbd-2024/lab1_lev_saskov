package ldr.server.storage.index;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.debatty.java.lsh.LSHSuperBit;
import ldr.client.domen.Embedding;

public class FastIndex implements IFastIndex {
    private static final Logger log = LoggerFactory.getLogger(FastIndex.class);
    private static final  int INITIAL_SEED = 42;
    private static final int STAGES = 5;
    private static final int BUCKETS = 15;

    private final ObjectMapper mapper = new ObjectMapper();
    private final LSHSuperBit lsh;
    private final Path location;
    private final List<Map<Integer, Set<Long>>> stageBuckets;
    private volatile boolean closed;

    public static FastIndex load(Config config) {
        return new FastIndex(config, loadFromFile(config.filePath));
    }

    // Use load.
    private FastIndex(Config config, List<Map<Integer, Set<Long>>> stageBuckets) {
        this.location = config.filePath();
        this.stageBuckets = stageBuckets;
        this.lsh = new LSHSuperBit(STAGES, BUCKETS, config.vectorLen(), INITIAL_SEED);
    }

    @Override
    public Set<Long> getNearest(double[] vector) {
        checkClosed();

        int[] buckets = getBuckets(vector);

        Set<Long> nearest = new HashSet<>(stageBuckets.get(0).get(buckets[0]));
        for (int i = 0; i < buckets.length; i++) {
            var stage = stageBuckets.get(i);
            nearest.retainAll(stage.get(buckets[i]));
        }

        return nearest;
    }

    @Override
    public void add(Embedding embedding) {
        checkClosed();

        int[] buckets = getBuckets(embedding.vector());
        for (int stageN = 0; stageN < buckets.length; stageN++) {
            var stage = stageBuckets.get(stageN);
            int bucket = buckets[stageN];
            Set<Long> ids = stage.get(buckets[stageN]);
            if (ids == null) {
                ids = new ConcurrentSkipListSet<>();
            }
            ids.add(embedding.id());
            stage.putIfAbsent(bucket, ids);
        }
    }

    @Override
    public void add(List<Embedding> embeddings) {
        embeddings.forEach(this::add);
    }

    @Override
    public void remove(long id) {
        checkClosed();

        for (var stage : stageBuckets) {
            for (Set<Long> bucket : stage.values()) {
                bucket.remove(id);
            }
        }
    }

    @Override
    public void remove(Collection<Long> ids) {
        ids.forEach(this::remove);
    }

    @Override
    public synchronized void flush() throws IOException {
        mapper.writeValue(location.toFile(), stageBuckets);
    }

    @Override
    public void close() throws IOException {
        checkClosed();
        closed = true;
        flush();
    }

    private void checkClosed() {
        if (closed) {
            throw new RuntimeException("Fast index already closed.");
        }
    }

    private int[] getBuckets(double[] vector) {
        return lsh.hash(vector);
    }

    private static List<Map<Integer, Set<Long>>> loadFromFile(Path location) {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<Integer, Set<Long>>> index = null;
        if (Files.exists(location)) {
            try {
                File jsonFile = location.toFile();
                index =  mapper.readValue(jsonFile, new TypeReference<>() {
                });
                log.info("Index found. Initialization from file.");
            } catch (IOException e) {
                log.info("Can't find index file, it will be created.");
            }
        }

        List<Map<Integer, Set<Long>>> result = new ArrayList<>(STAGES);
        if (index != null) {
            // Такие махинации, чтобы сделать экземпляр не какой-то там мапы и сета при загрузки из json,
            // а именно Concurrent.
            for (var stage : index) {
                ConcurrentHashMap<Integer, Set<Long>> map = new ConcurrentHashMap<>(stage.size());
                stage.forEach((key, value) -> map.put(key, new ConcurrentSkipListSet<>(value)));
                result.add(map);
            }
        } else {
            for (int i = 0; i < STAGES; i++) {
                result.add(new ConcurrentHashMap<>());
            }
        }
        return result;
    }

    /**
     * @param filePath - path of file, where index will be written. Will be written after close, if not presented.
     */
    public record Config(Path filePath, int vectorLen) {
    }
}
