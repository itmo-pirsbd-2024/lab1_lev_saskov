package ldr.server.storage.drive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ldr.client.domen.Embedding;
import ldr.server.serialization.my.DataEncoder;
import ldr.server.serialization.my.EmbeddingEncoder;
import ldr.server.util.PeekIterator;

public class HardDriveEmbeddings implements IHardDriveEmbeddings {
    private static final Logger log = LoggerFactory.getLogger(HardDriveEmbeddings.class);
    private static final String OFFSETS_FILENAME = "offsets.off";
    private static final String EMBEDDINGS_FILENAME = "embeddings.emb";
    private static final int EMBEDDINGS_BUFF_SIZE = 4096;
    private static final int OFFSETS_BUFF_SIZE = 64;

    private static final State YET_NOT_INIT_STATE = new State(-1, 0, null, null);

    private static final DataEncoder<Embedding> encoder = new EmbeddingEncoder();
    // Write lock blocks read too. I use write lock during changing state. Read lock for get operations.
    private final ReadWriteLock changeStateLock = new ReentrantReadWriteLock();

    private final Config config;
    private volatile State state;

    // Use load.
    private HardDriveEmbeddings(State state, Config config) {
        this.config = config;
        this.state = state;
    }

    public static HardDriveEmbeddings load(Config config) throws IOException {
        log.info("Loading drive embeddings with config: {}.", config);
        Optional<Integer> generation = Optional.empty();
        if (Files.notExists(config.location())) {
            log.info("Can't find created directory of drive embeddings, it will be created.");
        } else {
            generation = getLastGeneration(config.location());
        }

        if (generation.isPresent()) {
            State generationState = loadGeneration(generation.get(), config.location());
            log.info("Generation has been loaded successfully.");
            return new HardDriveEmbeddings(generationState, config);
        }

        log.info("Hard drive embeddings are empty, will be lazy initialized after save operation.");
        return new HardDriveEmbeddings(YET_NOT_INIT_STATE, config);
    }

    /**
     * Perform binary search, using offsets' and embeddings' files.
     * @return embedding or null if not found.
     */
    @Override
    public Embedding get(long id) {
        changeStateLock.readLock().lock();
        try {
            int first = 0;
            int last = state.embeddingsCount - 1;

            int position = (first + last) / 2;
            Embedding curEmbedding = readEmbedding(position);
            while (curEmbedding.id() != id && first <= last) {
                if (curEmbedding.id() > id) {
                    last = position - 1;
                } else {
                    first = position + 1;
                }
                position = (first + last) / 2;
                curEmbedding = readEmbedding(position);
            }

            return curEmbedding.id() == id ? curEmbedding : null;
        } finally {
            changeStateLock.readLock().unlock();
        }
    }

    private Embedding readEmbedding(int embeddingNum) {
        int offset = state.offsetsBB.getInt(embeddingNum * Integer.BYTES);
        return encoder.decode(state.embeddingsBB, offset).result();
    }

    @Override
    public List<Embedding> get(List<Long> ids) {
       List<Embedding> result = new ArrayList<>(ids.size());
       ids.forEach(id -> result.add(get(id)));
       return result;
    }

    /**
     * @param toSaveOut iterator of sorted embeddings.
     *                  Attention: OF SORTED EMBEDDINGS, only in this way work will be correct.
     */
    @Override
    public synchronized void save(Iterator<Embedding> toSaveOut) throws IOException {
        if (!toSaveOut.hasNext()) {
            return;
        }

        int newGeneration = state == YET_NOT_INIT_STATE ? 0 : state.generation() + 1;
        Path newGenerationLocation = config.location().resolve(Integer.toString(newGeneration));
        Files.createDirectories(config.location().resolve(Integer.toString(newGeneration)));

        // Мержим те, что пришли и те, что внутри.
        Iterator<Embedding> toSave = getMergedIterator(toSaveOut);
        saveGeneration(toSave, newGenerationLocation);

        int oldGeneration = state.generation;
        changeStateLock.writeLock().lock();
        try {
            // Переключаем стейт
            state = loadGeneration(newGeneration, config.location());
        } finally {
            changeStateLock.writeLock().unlock();
        }
        // Переключение прошло гладко, удаляем старый стейт.
        if (oldGeneration >= 0) {
            // Можно в отдельном потоке удаление сделать. Если захочется ускорить save.
            FileUtils.deleteDirectory(config.location().resolve(Integer.toString(oldGeneration)).toFile());
        }
    }

    /**
     * @return out смерженный с содержимым этого HardDriveEmbeddings итератор.
     * В случае дубликатов по id берется только из out.
     */
    private Iterator<Embedding> getMergedIterator(Iterator<Embedding> out) {
        PeekIterator outPeek = new PeekIterator(out);
        PeekIterator inPeek = new PeekIterator(getAllSaved());
        return new GravedMergedIterator(outPeek, inPeek);
    }

    /**
     * Get all saved from this.state.
     * No locking, use only during save operation to merge with embeddings from outer scope.
     */
    private Iterator<Embedding> getAllSaved() {
        return new Iterator<>() {
            int position;

            @Override
            public boolean hasNext() {
                return position < state.embeddingsCount;
            }

            @Override
            public Embedding next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                Embedding result = readEmbedding(position);
                position++;
                return result;
            }
        };
    }

    private static void saveGeneration(Iterator<Embedding> toSave, Path location) throws IOException {
        ByteBuffer embeddingsBuffer = ByteBuffer.allocate(EMBEDDINGS_BUFF_SIZE);
        ByteBuffer offsetsBuffer = ByteBuffer.allocate(OFFSETS_BUFF_SIZE);
        int bytesWritten = 0;
        int embeddingsCount = 0;

        try (
                SeekableByteChannel embeddingsChannel = Files.newByteChannel(location.resolve(EMBEDDINGS_FILENAME),
                        EnumSet.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW));
                SeekableByteChannel offsetsChannel = Files.newByteChannel(location.resolve(OFFSETS_FILENAME),
                        EnumSet.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW));
        ) {
            while (toSave.hasNext()) {
                byte[] embeddingBytes = encoder.encode(toSave.next());

                offsetsBuffer.putInt(bytesWritten);
                if (offsetsBuffer.position() == offsetsBuffer.capacity()) {
                    write(offsetsBuffer, offsetsChannel);
                }

                if (embeddingsBuffer.position() + embeddingBytes.length > embeddingsBuffer.capacity()) {
                    write(embeddingsBuffer, embeddingsChannel);
                }
                embeddingsBuffer.put(embeddingBytes);

                bytesWritten += embeddingBytes.length;
                embeddingsCount++;
            }
            write(embeddingsBuffer, embeddingsChannel);
            // Writing count of embedding in the end of file.
            offsetsBuffer.putInt(embeddingsCount);
            write(offsetsBuffer, offsetsChannel);
        }

        log.info("Saved generation in {}, size in bytes for embeddings: {}", location, bytesWritten);
    }

    /**
     * Write to file channel and clear buffer.
     */
    private static void write(ByteBuffer buffer, SeekableByteChannel channel) throws IOException {
        buffer.flip();
        channel.write(buffer);
        buffer.clear();
    }

    private static MappedByteBuffer mapFile(Path path) throws IOException {
        MappedByteBuffer mappedFile;
        try (FileChannel fileChannel = (FileChannel) Files.newByteChannel(path, StandardOpenOption.READ)) {
            mappedFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, Files.size(path));
        }

        return mappedFile;
    }

    private static State loadGeneration(int generation, Path location) throws IOException {
        Path genPath = location.resolve(Integer.toString(generation));
        log.info("Loading generation from {}", genPath);
        ByteBuffer offsetsBB = mapFile(genPath.resolve(OFFSETS_FILENAME));
        // Embeddings count has been written in the end of offsets' file.
        int embeddingsCount = offsetsBB.getInt(offsetsBB.capacity() - Integer.BYTES);
        ByteBuffer embeddingsBB = mapFile(genPath.resolve(EMBEDDINGS_FILENAME));
        return new State(generation, embeddingsCount, offsetsBB, embeddingsBB);
    }

    /**
     * Parse all files from directory, find numeric dirs (1, 2, 3, etc), choose dir with max number.
     */
    private static Optional<Integer> getLastGeneration(Path location) throws IOException {
        Optional<Integer> optionalMax;
        try(var files = Files.list(location)) {
            optionalMax = files
                    .filter(Files::isDirectory)
                    .map(f -> f.getFileName().toString())
                    .filter(StringUtils::isNumeric)
                    .map(Integer::parseInt)
                    .max(Integer::compareTo);
        }

        return optionalMax;
    }

    public record Config(Path location) {}

    private record State(int generation, int embeddingsCount, ByteBuffer offsetsBB, ByteBuffer embeddingsBB) {}
}
