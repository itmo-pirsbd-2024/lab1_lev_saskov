package jmh.read;

import java.io.IOException;
import ldr.client.domen.collection.VectorCollection;

public class ReadLoadTest {
    private static final int MAX_NEIGHBOURS_COUNT = 10;

    public static void main(String[] args) throws IOException {
        try (var collection = VectorCollection.load(new VectorCollection.Config(Prepare.locationCollection));) {
            for (int i = 0; i < 10000; i++) {
                var vector = Prepare.generateVector();
                var res = collection.query(vector, MAX_NEIGHBOURS_COUNT);
                System.out.println(i);
                System.out.println(res);
            }
        }
    }
}
