package ldr.server.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import ldr.client.domen.Embedding;
import ldr.server.storage.drive.HardDriveEmbeddings;
import ldr.server.storage.drive.IHardDriveEmbeddings;
import ldr.server.storage.mem.IMemoryEmbeddings;
import ldr.server.storage.mem.MemoryEmbeddings;


public class StorageManager implements IStorageManager {
    private static final Logger log = LoggerFactory.getLogger(StorageManager.class);
    private final IHardDriveEmbeddings inDrive;
    private final MemoryEmbeddings.Config memConfig;
    private IMemoryEmbeddings inMem;
    private volatile boolean closed;

    public static StorageManager load(Config config) throws IOException {
        var hardDriveConfig = new HardDriveEmbeddings.Config(config.location);
        IHardDriveEmbeddings inDrive = HardDriveEmbeddings.load(hardDriveConfig);
        return new StorageManager(config.flushThresholdBytes, config.metaEntrySize, inDrive);
    }

    // Use load.
    private StorageManager(int flushThresholdBytes, int metaEntrySize, IHardDriveEmbeddings inDrive) {
        this.inDrive = inDrive;
        this.memConfig = new MemoryEmbeddings.Config(flushThresholdBytes, metaEntrySize);
        this.inMem = new MemoryEmbeddings(memConfig, this::flushCallback);
    }

    @Override
    public Embedding get(long id) {
        checkClosed();
        Embedding result = inMem.get(id);
        if (result == null) {
            return inDrive.get(id);
        } else  if (isGrave(result)) {
            return null;
        }

        return result;
    }

    @Override
    public List<Embedding> get(List<Long> ids) {
        List<Embedding> result = new ArrayList<>(ids.size());
        for (long id : ids) {
            Embedding embedding = get(id);
            if (embedding != null) {
                result.add(embedding);
            }
        }

        return result;
    }

    @Override
    public void add(Embedding embedding) {
        checkClosed();
        inMem.add(embedding);
    }

    @Override
    public void add(List<Embedding> embeddings) {
        embeddings.forEach(this::add);
    }

    @Override
    public void remove(long id) {
        checkClosed();
        add(createGrave(id));
    }

    @Override
    public void remove(Collection<Long> ids) {
        ids.forEach(this::remove);
    }

    @Override
    public void close() throws IOException {
        checkClosed();
        closed = true;
        flush();
    }

    @Override
    public synchronized void flush() throws IOException {
        inDrive.save(inMem.getAll());
        inMem = new MemoryEmbeddings(memConfig, this::flushCallback);
    }

    private synchronized void flushCallback(List<Embedding> embeddings) {
        // У нас есть небольшая гонка перед тем, как вызвать этот метод внутри add в StorageEmbeddings,
        // Сейчас мы в последовательном коде и можем еще раз все проверить.
        if (inMem.isNeedFlush()) {
            try {
                flush();
            } catch (IOException e) {
                log.error("Exception during flush. Flush is aborted, if flush won't be successful later, " +
                        "then you face flush loop, and add operation won't be work correctly. " +
                        "So get operations could be inconsistent.", e);
            }
        }

        // Кладем Embedding-и, из-за которых мы до этого выходили за границы размера. После флаша он должен влезть.
        inMem.add(embeddings);
    }

    private void checkClosed() {
        if (closed) {
            throw new RuntimeException("Storage manager already closed");
        }
    }

    @SuppressWarnings("ConstantConditions") // null for Nonnullable is correct. It is identification of graves.
    @VisibleForTesting
    static Embedding createGrave(long id) {
        return new Embedding(id, null, null);
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean isGrave(Embedding embedding) {
        return embedding.vector() == null && embedding.metas() == null;
    }

    public record Config(Path location, int flushThresholdBytes, int metaEntrySize) {
    }
}
