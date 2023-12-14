package server.local;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocalWorkerTest {

    @Test
    void testGetCollectionsToSend() {
        var domains = Set.of(1, 2, 4, 244);
        var correct = List.of("coll_1", "coll_2", "dada_2", "coll_4", "da_244", "collection5_1");
        var all = new ArrayList<>(correct);
        all.addAll(List.of("", "dckaopcam", "dada_d", "dw_32", "coll_31", "44", "coll5", "coll_5", "collection1_5"));

        var result = LocalWorker.getCollectionsToSend(all, domains);
        assertEquals(correct, result);
    }
}