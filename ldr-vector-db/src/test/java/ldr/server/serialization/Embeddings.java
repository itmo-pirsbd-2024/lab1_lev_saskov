package ldr.server.serialization;

import java.util.List;
import java.util.Map;

import ldr.client.domen.Embedding;

public class Embeddings {
    public static List<Embedding> examples = List.of(
            new Embedding(10, new double[]{11.0, 12.5, 13.0},
                    Map.of("metaKey1", "metaVal1", "metaKey2", "metaVal1")),
            new Embedding(50, new double[]{14.7, 132.5, 133.8, 32.3},
                    Map.of("da", "daVal", "ne", "neVal"))
    );
}
