package ldr.server;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import ldr.client.domen.Embedding;

public class TestUtils {
    public static final Path resourcesPath = Paths.get("src", "test", "resources");

    // Use only for not concurrent tests. For concurrent use ThreadLocalRandom.
    private static final Random random = new Random(10);

    public static List<Embedding> generateManyEmbeddings(int count, int maxVectorLen, int maxMetaSize) {
        return generateManyEmbeddings(count, maxVectorLen, maxMetaSize, false);
    }

    /**
     * @param serialId - флаг на то, чтобы id были последовательными и начинались с 0.
     */
    public static List<Embedding> generateManyEmbeddings(int count, int maxVectorLen, int maxMetaSize, boolean serialId) {
        List<Embedding> result = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            result.add(
                    new Embedding(
                            serialId ? i : ThreadLocalRandom.current().nextInt(),
                            generateVector(i % maxVectorLen),
                            generateMeta(i % maxMetaSize)
                    )
            );
        }

        return result;
    }

    /**
     * Generate near vectors with random id, дельта по каждой координате < coordinateDeltaBound.
     */
    public static List<Embedding> generateNearEmbeddings(int count, int vectorLen, double coordinateDeltaBound) {
        double[] mainVector =  generateVector(vectorLen);

        List<Embedding> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(new Embedding(random.nextInt(), generateNearVector(mainVector, coordinateDeltaBound), new HashMap<>()));
        }

        return result;
    }

    /**
     * Default size list of embeddings.
     */
    public static List<Embedding> generateManyEmbeddings() {
        return generateManyEmbeddings(100, 20, 10);
    }

    public static int randomInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    public static List<Embedding> getRandomSubList(int subListSize, List<Embedding> list) {
        if (subListSize > list.size()) {
            throw new IndexOutOfBoundsException(
                    String.format("Sublist is bigger then list. sublist size: %d, list size: %d",
                            subListSize, list.size())
            );
        }

        List<Embedding> subList = new ArrayList<>(subListSize);
        for (int i = 0; i < subListSize; i++) {
            subList.add(list.get(randomInt(list.size())));
        }

        subList.sort(Comparator.comparingLong(Embedding::id));

        return subList;
    }

    private static double[] generateNearVector(double[] mainVector, double coordinateDelta) {
        double[] result = new double[mainVector.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = mainVector[i] + random.nextDouble(coordinateDelta);
        }

        return result;
    }

    private static double[] generateVector(int vectorLen) {
        double[] vector = new double[vectorLen];
        for (int i = 0; i < vectorLen; i++) {
            vector[i] = ThreadLocalRandom.current().nextDouble(1000.0);
        }

        return vector;
    }

    private static Map<String, String> generateMeta(int size) {
        Map<String, String> meta = new HashMap<>();

        for (int i = 0; i < size; i++) {
            meta.put(
                    ("key" + i).repeat(i),
                    ("val" + i).repeat(i)
            );
        }

        return meta;
    }
}
