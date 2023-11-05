package ldr.client.domen.collection;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ldr.client.domen.DistancedEmbedding;
import ldr.client.domen.Embedding;
import ldr.client.domen.VectorCollectionResult;
import ldr.server.storage.IEmbeddingKeeper;
import ldr.server.storage.IEmbeddingRemover;
import ldr.server.storage.IHardMemory;
import ldr.server.storage.IStorageManager;
import ldr.server.storage.StorageManager;
import ldr.server.storage.index.FastIndex;
import ldr.server.storage.index.IFastIndex;
import ldr.server.util.FixedSizePriorityQueue;

public class VectorCollection implements IVectorCollection {
    private static final Logger log = LoggerFactory.getLogger(VectorCollection.class);

    private static final String CONFIG_FILENAME = "config.json";
    private static final String STORAGE_FOLDER = "storage";
    private static final String INDEX_FILENAME = "index.json";
    private static final int FLUSH_THRESHOLD_BYTES = 300_000;
    private static final int META_ENTRY_SIZE = 15;

    private final IStorageManager storage;
    private final IFastIndex index;
    private final IHardMemory[] hardMemory;

    private volatile boolean closed;

    public static VectorCollection load(Config config) throws IOException {
        Path location = config.location();
        Path configFile = config.location().resolve(CONFIG_FILENAME);
        if (!Files.exists(configFile)) {
            log.info("Can't find snapshot. New collection will be created.");
            createCollection(location, configFile, config);
        } else {
            log.info("Snapshot found, will be loaded.");
            Config savedConfig = loadConfig(configFile);
            if (!savedConfig.equals(config)) {
                if (!config.mustBeReloadedFromFile()) {
                    log.error("Saved config: {} is not equal to new config: {}. Saved config will be used.",
                            savedConfig, config);
                }
                config = savedConfig;
            }
        }

        var storageConfig =
                new StorageManager.Config(location.resolve(STORAGE_FOLDER), FLUSH_THRESHOLD_BYTES, META_ENTRY_SIZE);

        IStorageManager storage = StorageManager.load(storageConfig);
        IFastIndex index = FastIndex.load(new FastIndex.Config(location.resolve(INDEX_FILENAME), config.vectorLen()));

        log.info("Collection was loaded.");
        return new VectorCollection(
                storage,
                index
        );
    }

    private VectorCollection(IStorageManager storage, IFastIndex index) {
        this.storage = storage;
        this.index = index;
        this.hardMemory = new IHardMemory[]{storage, index};
    }

    @Override
    public void add(Embedding embedding) {
        checkClosed();

        for (IEmbeddingKeeper mem : hardMemory) {
            mem.add(embedding);
        }
    }

    @Override
    public void add(List<Embedding> embeddings) {
        checkClosed();

        for (IEmbeddingKeeper mem : hardMemory) {
            mem.add(embeddings);
        }
    }

    @Override
    public VectorCollectionResult query(double[] vector, int maxNeighborsCount) {
        checkClosed();

        Set<Long> nearest = index.getNearest(vector);
        if (nearest.isEmpty()) {
            return VectorCollectionResult.EMPTY;
        }

        List<Embedding> embeddings = storage.get(nearest.stream().toList());
        FixedSizePriorityQueue<DistancedEmbedding> queue = new FixedSizePriorityQueue<>(maxNeighborsCount);
        embeddings.forEach(e -> queue.add(new DistancedEmbedding(e, getDistance(vector, e.vector()))));

        List<DistancedEmbedding> results = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) {
            results.add(queue.poll());
        }

        return new VectorCollectionResult(results);
    }

    /**
     * Считает расстояние между 2мя векторами.
     * При подсчете квадратной суммы разностей нет защиты от переполнения,
     * поэтому сильно далекие вектора передавать нельзя.
     */
    private double getDistance(double[] a, double[] b) {
        double diffSquareSum = 0;
        for (int i = 0; i < a.length; i++) {
            diffSquareSum += (a[i] - b[i]) * (a[i] - b[i]);
        }

        return Math.sqrt(diffSquareSum);
    }

    @Override
    public VectorCollectionResult query(double[] vector, int maxNeighborsCount, String filter) {
        throw new NotImplementedException();
    }

    @Override
    public void delete(long id) {
        checkClosed();

        for (IEmbeddingRemover mem : hardMemory) {
            mem.remove(id);
        }
    }

    @Override
    public void delete(Collection<Long> ids) {
        checkClosed();

        for (IEmbeddingRemover mem : hardMemory) {
            mem.remove(ids);
        }
    }

    @Override
    public void delete(String filter) {
        throw new NotImplementedException();
    }

    @Override
    public void close() throws IOException {
        checkClosed();
        closed = true;

        for (Closeable mem : hardMemory) {
            mem.close();
        }
    }

    private void checkClosed() {
        if (closed) {
            throw new RuntimeException("Collection already closed.");
        }
    }

    private static void createCollection(Path location, Path configFile, Config config) throws IOException {
        if (Files.exists(location)) {
            log.warn("Dir of collection already exists, but config not found. " +
                    "New collection will be created with deleting old dir.");
            FileUtils.deleteDirectory(location.toFile());
        }
        Files.createDirectory(location);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(configFile.toFile(), config);
        log.info("Config saved.");
    }

    private static Config loadConfig(Path configFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Config config = mapper.readValue(configFile.toFile(), new TypeReference<>() {
        });
        log.info("Config was loaded: {}", config);
        return config;
    }

    /**
     * @param location of collection. Will be created if not presented.
     */
    public record Config(Path location, int vectorLen) {
        private static final int MUST_BE_FROM_FILE_V_LEN = -1;
        public Config(Path location) {
            this(location, MUST_BE_FROM_FILE_V_LEN);
        }

        private boolean mustBeReloadedFromFile() {
            return vectorLen == MUST_BE_FROM_FILE_V_LEN;
        }
    }
}
